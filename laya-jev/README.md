# Laya Jev 兼容服务

基于 FastAPI 和 [Laya 多语言模型](https://huggingface.co/convaiinnovations/laya-multilingual) 的本地推理服务，提供兼容 Jev 请求格式的 `POST /v1/systemone` 接口，可用于分类、布尔判断和评分。支持 CPU 与 NVIDIA GPU，模型在部署机器上运行。

IntelliConnect 可通过现有 `JevClient` 接入。`jev-*` 等模型名称是兼容别名，实际使用的模型为 `laya-multilingual`。

## 环境要求

- Python 3.11，Windows 或 Linux。
- PyTorch 2.6.0；其他依赖版本见 [requirements.txt](requirements.txt)。
- 至少 4 GiB 可用系统内存；多个模型副本和长输入需要更多内存。GPU 模式也会检查系统内存预算。
- GPU 模式需要受所选 PyTorch CUDA 版本支持的 NVIDIA 显卡及驱动。显存需求取决于输入长度、问题数量和模型副本数，建议从单副本开始。
- 预留模型、虚拟环境和下载缓存空间。模型权重约 614 MiB，tokenizer 约 33 MiB，CUDA 版 PyTorch 及其依赖还需要数 GiB 空间。

下文 GPU 安装命令以 CUDA 11.8 wheel 为例。根据显卡和驱动选择 [PyTorch 官方提供的版本](https://pytorch.org/get-started/previous-versions/#v260)，较新的 GPU 架构可能需要更新的 PyTorch/CUDA 组合及相应依赖适配。官方 wheel 包含 CUDA 运行时，运行本服务通常无需单独安装 CUDA Toolkit，但必须安装兼容的 NVIDIA 驱动。`nvidia-smi` 显示的 CUDA 版本表示驱动支持能力，不代表虚拟环境已经安装 CUDA 版 PyTorch。

## Windows 安装与运行

在仓库的 `laya-jev` 目录中打开 PowerShell，创建虚拟环境：

```powershell
py -3.11 -m venv .venv
.\.venv\Scripts\python.exe -m pip install --upgrade pip
```

根据推理设备选择一条 PyTorch 安装命令。

CPU：

```powershell
.\.venv\Scripts\python.exe -m pip install 'torch==2.6.0+cpu' --index-url https://download.pytorch.org/whl/cpu
```

NVIDIA GPU：

```powershell
nvidia-smi
.\.venv\Scripts\python.exe -m pip install 'torch==2.6.0+cu118' --index-url https://download.pytorch.org/whl/cu118
.\.venv\Scripts\python.exe -c "import torch; print(torch.__version__); assert torch.cuda.is_available(), 'CUDA unavailable'; print(torch.cuda.get_device_name(0))"
```

安装服务依赖并下载模型：

```powershell
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe download_model.py
```

选择设备启动：

```powershell
# CPU
.\start.ps1 -Device cpu

# 或 NVIDIA GPU
.\start.ps1 -Device cuda
```

`start.ps1` 在后台启动守护进程，自动优先使用已下载并校验的本地模型，等待健康检查成功后返回。默认地址为 `http://127.0.0.1:8001`，可通过 `-Port 8002` 修改端口。此方式不注册 Windows 开机自启服务。

```powershell
Invoke-RestMethod http://127.0.0.1:8001/health
```

停止服务：

```powershell
.\stop.ps1
```

停止脚本发出退出请求后立即返回。切换 CPU/GPU、修改配置或升级依赖前，等待守护进程退出、端口释放，再启动服务。CUDA 版 PyTorch 同时支持 CPU 推理，日常切换只需改变 `-Device`，无需重复安装依赖。

## Linux 安装与运行

以下示例使用 systemd、服务用户 `laya-jev`，并假设本仓库已克隆或解压到 `/opt/laya-jev`，因此适配器目录为 `/opt/laya-jev/laya-jev`。使用其他路径时，同步修改命令、环境文件和 unit 中的路径。

### 1. 创建运行环境

先安装 Python 3.11 及其 `venv` 支持。不同发行版的软件源提供的 Python 版本不同，请通过发行版支持的方式安装；确认 `python3.11 --version` 可用后继续。

```bash
id -u laya-jev >/dev/null 2>&1 || sudo useradd --system --user-group \
  --home-dir /opt/laya-jev --shell /usr/sbin/nologin laya-jev
sudo chown -R laya-jev:laya-jev /opt/laya-jev/laya-jev
cd /opt/laya-jev/laya-jev
sudo -u laya-jev -H python3.11 -m venv .venv
sudo -u laya-jev -H .venv/bin/python -m pip install --upgrade pip
```

### 2. 安装 CPU 或 GPU 依赖

CPU 部署：

```bash
sudo -u laya-jev -H .venv/bin/python -m pip install \
  'torch==2.6.0+cpu' --index-url https://download.pytorch.org/whl/cpu
```

NVIDIA GPU 部署：先按照发行版或云平台说明安装 NVIDIA 驱动，完成必要的重启，确保 `nvidia-smi` 能列出目标显卡。再安装 CUDA 版 PyTorch：

```bash
nvidia-smi
sudo -u laya-jev -H .venv/bin/python -m pip install \
  'torch==2.6.0+cu118' --index-url https://download.pytorch.org/whl/cu118
sudo -u laya-jev -H .venv/bin/python -c \
  "import torch; print(torch.__version__); print('CUDA runtime:', torch.version.cuda); assert torch.cuda.is_available(), 'CUDA unavailable'; print(torch.cuda.get_device_name(0))"
```

使用服务用户执行 CUDA 检查，可以同时确认它能够访问 GPU。若 `torch.version.cuda` 为 `None`，说明安装的是 CPU 版；若安装了 CUDA 版但 GPU 不可用，检查驱动、设备权限和 `CUDA_VISIBLE_DEVICES`。在容器中部署时，还需由宿主机安装 NVIDIA Container Toolkit，并向容器暴露 GPU。

完成所选 PyTorch 的安装后，安装公共依赖：

```bash
sudo -u laya-jev -H .venv/bin/python -m pip install -r requirements.txt
```

### 3. 下载模型

```bash
sudo -u laya-jev -H .venv/bin/python download_model.py
```

模型下载到 `.runtime/models/multilingual`。下载脚本固定模型 revision，并自动校验权重和 tokenizer 的 SHA-256。有关缓存、镜像和离线部署，见下文“模型文件”。

### 4. 配置 systemd

复制仓库提供的 [环境模板](laya-jev.env.example) 和 [服务模板](laya-jev.service.example)：

```bash
sudo install -d -o root -g laya-jev -m 0750 /etc/laya-jev
sudo install -o root -g laya-jev -m 0640 \
  laya-jev.env.example /etc/laya-jev/laya-jev.env
sudo install -o root -g root -m 0644 \
  laya-jev.service.example /etc/systemd/system/laya-jev.service
sudoedit /etc/laya-jev/laya-jev.env
```

GPU 部署时，将环境文件中的对应项设为：

```ini
LAYA_DEVICE=cuda
LAYA_WORKERS=1
LAYA_MODEL_PATH=/opt/laya-jev/laya-jev/.runtime/models/multilingual
HF_HOME=/opt/laya-jev/laya-jev/.runtime/huggingface
CUDA_VISIBLE_DEVICES=0
```

`CUDA_VISIBLE_DEVICES=0` 选择第一张 GPU；多卡机器可根据 `nvidia-smi -L` 改为其他编号或 GPU UUID。选择一张卡后，服务用 `LAYA_DEVICE=cuda` 访问它。`LAYA_WORKERS` 表示独立模型副本数，增加该值会增加所选 GPU 的显存占用，不会自动把副本分配到不同显卡。

CPU 部署使用 `LAYA_DEVICE=cpu`，删除或注释 `LAYA_WORKERS`、`LAYA_THREADS` 即可恢复自动资源分配。环境文件使用 `KEY=value` 格式，无需 `export`；程序本身不会自动读取 `.env`，这里由 systemd 注入配置。

启动并设置开机自启：

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now laya-jev.service
systemctl status laya-jev.service --no-pager
curl --fail --noproxy '*' http://127.0.0.1:8001/health
```

服务模板使用 `Type=simple`，systemd 显示 `active` 时模型可能还在加载。以 `/health` 返回 HTTP 200 且 `status` 为 `ok` 作为可用标志；默认模型加载等待时限为 300 秒，由守护进程的 `LAYA_STARTUP_TIMEOUT_SECONDS` 控制。

启动时显式要求 CUDA，但模型未能加载到 GPU，服务会报错退出。`/health` 中的 `device` 表示配置的设备；运行中 GPU 的实际使用情况还可通过 `nvidia-smi` 和服务日志查看。

### 5. 日志、切换与更新

```bash
# 守护进程日志
journalctl -u laya-jev.service -n 100 --no-pager

# 模型加载和推理日志
sudo tail -n 100 /opt/laya-jev/laya-jev/.runtime/service.log

# GPU 状态
nvidia-smi
```

切换 CPU/GPU 时，修改 `/etc/laya-jev/laya-jev.env` 中的 `LAYA_DEVICE`，调整副本数后重启：

```bash
sudo systemctl restart laya-jev.service
curl --fail --noproxy '*' http://127.0.0.1:8001/health
```

重启后需等待模型重新加载。仅修改环境文件无需 `daemon-reload`；修改 unit 后需先执行 `sudo systemctl daemon-reload`。终端中的 `export LAYA_DEVICE=cuda` 不会改变已由 systemd 管理的服务配置。

更新代码或依赖前，先执行 `sudo systemctl stop laya-jev.service`；更新完成后执行 `sudo systemctl start laya-jev.service`。如需停用并取消开机自启：

```bash
sudo systemctl disable --now laya-jev.service
```

需要前台排查启动错误时，先停止 systemd 服务，再在适配器目录执行：

```bash
sudo -u laya-jev -H env \
  LAYA_DEVICE=cuda LAYA_WORKERS=1 CUDA_VISIBLE_DEVICES=0 \
  LAYA_MODEL_PATH=/opt/laya-jev/laya-jev/.runtime/models/multilingual \
  HF_HOME=/opt/laya-jev/laya-jev/.runtime/huggingface \
  .venv/bin/python app.py
```

CPU 排障时将 `LAYA_DEVICE` 改为 `cpu`。前台运行 `app.py` 不提供自动重启；常驻服务使用模板中的 `supervise.py`。保持单 Uvicorn worker，由内部模型池负责并发，避免重复加载整个模型池。

## 模型文件

默认使用 `convaiinnovations/laya-multilingual`，下载脚本固定 revision 为 `052592a15d198d9ad47da779604259b10b47b7aa`。首次部署运行 `download_model.py`；模型下载失败或校验失败时，修复下载后再启动服务。

默认目录结构：

```text
laya-jev/
├── .venv/                       # Python 虚拟环境
└── .runtime/
    ├── models/multilingual/     # 模型、tokenizer 和 verified.json
    ├── huggingface/             # 下载缓存
    └── service.log              # 守护进程收集的模型服务日志
```

`.venv/` 和 `.runtime/` 已加入 `.gitignore`。离线部署时，可从已下载模型的机器复制完整的 `.runtime/models/multilingual` 目录，并在目标机器运行 `verify_model.py`。Python 依赖需按目标操作系统和设备单独安装，虚拟环境不宜跨平台复制。

网络无法访问 Hugging Face 时，可在下载前设置可信镜像地址，例如 PowerShell 的 `$env:HF_ENDPOINT='https://hf-mirror.com'` 或 Bash 的 `export HF_ENDPOINT=https://hf-mirror.com`。模型下载无需 Hugging Face Token。

## API 与 IntelliConnect 接入

服务提供两个接口：

| 接口 | 用途 |
|---|---|
| `GET /health` | 查看服务状态、配置设备、资源分配和活动请求数 |
| `POST /v1/systemone` | 提交状态与问题，返回答案、概率、置信度和 token 用量 |

请求示例：

```json
{
  "model": "jev-latest",
  "state": {"message": "今天很开心"},
  "questions": {
    "emotion": {
      "type": "choice",
      "instructions": "判断当前消息表达的情绪",
      "criteria": {"happy": "开心", "sad": "难过", "neutral": "中性"}
    }
  }
}
```

支持 `noul`、`choice`、`score` 问题。`state` 和 `instructions` 可使用非空字符串、对象或数组。`jev-*`、`multilingual`、`laya-multilingual` 和完整模型 ID 都映射到同一个多语言模型，响应中的 `model` 为 `laya-multilingual`，`usage.output_tokens` 为 0。

IntelliConnect 的配置示例：

```yaml
ai:
  decision-model:
    base-url: http://127.0.0.1:8001
    key: ${JEV_API_KEY}
    model: jev-latest
  emotionTool-llm: decision-model
```

Java 客户端要求非空 Key。默认本地服务不校验 Key，可将 `JEV_API_KEY` 设为非空的本地占位值；启用服务端 `LAYA_API_KEY` 后，Java 的 Key 必须与之相同，其他调用方需发送 `Authorization: Bearer <key>`。

跨机器访问时配置 `LAYA_HOST=0.0.0.0` 或服务器内网地址，并设置 `LAYA_API_KEY`。客户端的 base URL 应使用服务器实际地址；容器内的 `127.0.0.1` 指向容器自身。远程部署应限制端口访问范围，对公网访问使用 HTTPS 反向代理。

## 配置

配置在服务启动时读取，修改后需重启。

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `LAYA_HOST` / `LAYA_PORT` | `127.0.0.1` / `8001` | 监听地址和端口；Windows 启动脚本通过 `-Port` 设置端口 |
| `LAYA_DEVICE` | `cpu` | 推理设备：`cpu` 或 `cuda`；Windows 启动脚本通过 `-Device` 设置 |
| `CUDA_VISIBLE_DEVICES` | 由系统环境决定 | CUDA 可见设备，GPU 部署可设为目标卡编号或 UUID |
| `LAYA_MODEL_PATH` | 公共多语言模型 ID | 本地模型路径；Windows 启动脚本自动优先选择已校验的本地模型，Linux 示例显式配置路径 |
| `LAYA_API_KEY` | 空 | 可选 Bearer 鉴权 |
| `LAYA_WORKERS` | CPU 自动计算；GPU 为 `1` | 独立模型副本数 |
| `LAYA_THREADS` | 自动计算 | 每个模型的 CPU 计算线程数 |
| `LAYA_REQUEST_TIMEOUT_SECONDS` | `8` | HTTP 请求等待时限，单位为秒 |
| `LAYA_INFERENCE_TIMEOUT_SECONDS` | `max(60, HTTP等待时限)` | 推理执行时限，必须为有限值且不小于 HTTP 等待时限 |
| `LAYA_BUSY_RETRY_SECONDS` | `3` | 槽位占满或请求超时时的 `Retry-After` 秒数 |
| `LAYA_STARTUP_TIMEOUT_SECONDS` | `300` | 守护进程等待模型加载的时限 |
| `LAYA_MAX_TOKENS` | `8192` | 每个问题包含上下文的总 token 上限 |
| `LAYA_MAX_QUESTIONS` | `32` | 每次请求的问题数上限 |
| `LAYA_MAX_BATCH_TOKENS` | `16384` | 按最长序列补齐后的批次 token 上限 |
| `HF_HOME` | `.runtime/huggingface` | 模型下载缓存路径 |

CPU 模式按可用物理核心和系统内存分配资源，预留约 25% 核心和 2 GiB 内存，默认最多 4 个模型副本。每副本按 2 GiB 内存估算，实际消耗随输入变化。手动设置 `LAYA_WORKERS`、`LAYA_THREADS` 时仍需满足 CPU 和内存预算；GPU 显存预算需由部署者根据负载控制。

## 超时与常见问题

所有模型槽位占满时，请求立即返回 `529` 和 `Retry-After`，不无限排队。HTTP 等待超时也返回 `529`；底层推理继续占用原槽位，完成后自动释放，其他空闲槽位可继续工作。超过推理执行时限仍未结束时，健康检查返回 `503` 并请求守护进程重启模型服务。重载期间客户端可能遇到连接中断。

| 现象 | 处理方式 |
|---|---|
| `CUDA requested but not available` | 使用服务对应虚拟环境检查 PyTorch 是否为 CUDA 版、驱动是否可用、服务用户是否可访问 GPU |
| 启动成功但 GPU 占用消失、推理变慢 | 检查日志中的 GPU 错误或 CPU 回退提示；上游 Laya 可能在运行中遇到显存不足或 CUDA 错误后回退 CPU，`/health.device` 仍显示配置值。减少输入或副本数、释放显存后重启 |
| 请求频繁超过 8 秒 | 缩短上下文或减少单次问题数，检查 CPU/GPU 负载；提高 HTTP 等待时限时，同时调整客户端读取超时。只提高执行时限不会延长 HTTP 等待 |
| `401` | 检查 Bearer Key 与 `LAYA_API_KEY` 是否一致 |
| `422` | 检查请求格式及 token、问题数限制 |
| `529` 或健康检查 `503` | 查看是否容量不足、超时或内存耗尽；结合 `.runtime/service.log` 排查 |
| systemd 已启动但接口无法连接 | 模型可能仍在加载，查看服务日志并等待 `/health`；远程访问还需检查监听地址和防火墙 |

Java 客户端会对 `429`、`529` 最多重试两次。长请求反复超时时，重试可能增加计算负载，应先调整输入和资源配置。

单个选项描述由上游保留前 48 tokens，建议使用简短、清晰的描述。超长输入返回 `422`，不会自动裁剪 state 或 instructions；批量问题按最长序列补齐。API 格式兼容不保证不同模型的分类结果、概率或延迟相同。
