#!/usr/bin/env sh

kind="${1:-complete}"
hook_input="$(cat 2>/dev/null || true)"
config_file=".codex/coding_agent_session.yaml"

agent_status() {
    case "$1" in
        ready|READY) printf 'READY' ;;
        thinking|THINKING) printf 'THINKING' ;;
        working|WORKING) printf 'WORKING' ;;
        processing|PROCESSING) printf 'PROCESSING' ;;
        warning|WARNING) printf 'WARNING' ;;
        finished|FINISHED|complete|COMPLETE) printf 'FINISHED' ;;
        error|ERROR) printf 'ERROR' ;;
        offline|OFFLINE) printf 'OFFLINE' ;;
        *) printf 'FINISHED' ;;
    esac
}

terminal_bell() {
    printf '\a' >&2
}

play_macos() {
    if command -v osascript >/dev/null 2>&1; then
        osascript -e 'beep 2' >/dev/null 2>&1 && return 0
    fi

    if command -v afplay >/dev/null 2>&1; then
        if [ "$kind" = "warning" ] && [ -f /System/Library/Sounds/Basso.aiff ]; then
            afplay /System/Library/Sounds/Basso.aiff >/dev/null 2>&1 && return 0
        fi

        if [ -f /System/Library/Sounds/Glass.aiff ]; then
            afplay /System/Library/Sounds/Glass.aiff >/dev/null 2>&1 && return 0
        fi
    fi

    return 1
}

play_linux() {
    sound_name="complete"
    [ "$kind" = "warning" ] && sound_name="dialog-warning"

    if command -v canberra-gtk-play >/dev/null 2>&1; then
        canberra-gtk-play -i "$sound_name" >/dev/null 2>&1 && return 0
    fi

    if command -v paplay >/dev/null 2>&1; then
        for file in \
            "/usr/share/sounds/freedesktop/stereo/${sound_name}.oga" \
            "/usr/share/sounds/freedesktop/stereo/dialog-information.oga" \
            "/usr/share/sounds/ubuntu/stereo/${sound_name}.ogg"
        do
            [ -f "$file" ] && paplay "$file" >/dev/null 2>&1 && return 0
        done
    fi

    if command -v aplay >/dev/null 2>&1 && [ -f /usr/share/sounds/alsa/Front_Center.wav ]; then
        aplay -q /usr/share/sounds/alsa/Front_Center.wav >/dev/null 2>&1 && return 0
    fi

    return 1
}

yaml_value() {
    key="$1"
    [ -f "$config_file" ] || return 0
    awk -v key="$key" '
        /^[[:space:]]*#/ { next }
        /^[[:space:]]*$/ { next }
        {
            pos=index($0, ":")
            if (pos == 0) {
                next
            }
            k=substr($0, 1, pos - 1)
            gsub(/^[[:space:]]+|[[:space:]]+$/, "", k)
            if (k == key) {
                value=substr($0, pos + 1)
                gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
                gsub(/^["'\''"]|["'\''"]$/, "", value)
                print value
                exit
            }
        }
    ' "$config_file"
}

json_value() {
    key="$1"
    printf '%s' "$hook_input" | sed -n "s/.*\"$key\"[[:space:]]*:[[:space:]]*\"\\([^\"]*\\)\".*/\\1/p" | head -n 1
}

send_coding_agent_session_status() {
    status="$1"
    [ -f "$config_file" ] || return 0

    enabled="$(yaml_value enabled)"
    [ "$enabled" = "false" ] && return 0

    backend_url="$(yaml_value backendUrl)"
    agent_id="$(yaml_value agentId)"
    pair_code="$(yaml_value pairCode)"

    [ -n "$backend_url" ] || return 0
    [ -n "$agent_id" ] || return 0
    [ -n "$pair_code" ] || return 0
    case "$agent_id" in
        *[!0-9]*|"") return 0 ;;
    esac

    session_id="$(yaml_value sessionId)"
    [ -n "$session_id" ] || session_id="$(json_value session_id)"
    [ -n "$session_id" ] || session_id="$(json_value sessionId)"
    [ -n "$session_id" ] || session_id="$(json_value conversation_id)"
    [ -n "$session_id" ] || session_id="$(json_value conversationId)"
    [ -n "$session_id" ] || session_id="$(json_value thread_id)"
    [ -n "$session_id" ] || session_id="$(json_value threadId)"
    [ -n "$session_id" ] || session_id="${CODEX_SESSION_ID:-}"
    [ -n "$session_id" ] || session_id="${CODEX_THREAD_ID:-}"
    [ -n "$session_id" ] || session_id="$(basename "$(pwd)")"

    cwd_value="$(yaml_value cwd)"
    [ -n "$cwd_value" ] || cwd_value="$(json_value cwd)"
    [ -n "$cwd_value" ] || cwd_value="$(json_value workspace)"
    [ -n "$cwd_value" ] || cwd_value="$(json_value workspace_dir)"
    [ -n "$cwd_value" ] || cwd_value="$(json_value workspaceDir)"
    [ -n "$cwd_value" ] || cwd_value="$(pwd)"

    upload_url="$(yaml_value uploadUrl)"
    if [ -z "$upload_url" ]; then
        upload_url="${backend_url%/}/api/v2/codingAgentSession/upload"
    fi

    json_body=$(printf '{"agentId":%s,"pairCode":"%s","sessionId":"%s","cwd":"%s","finalStatus":"%s"}' \
        "$agent_id" \
        "$(printf '%s' "$pair_code" | sed 's/\\/\\\\/g; s/"/\\"/g')" \
        "$(printf '%s' "$session_id" | sed 's/\\/\\\\/g; s/"/\\"/g')" \
        "$(printf '%s' "$cwd_value" | sed 's/\\/\\\\/g; s/"/\\"/g')" \
        "$status")

    if command -v curl >/dev/null 2>&1; then
        curl -fsS --max-time 2 -H 'Content-Type: application/json' -d "$json_body" "$upload_url" >/dev/null 2>&1 || true
    elif command -v wget >/dev/null 2>&1; then
        wget -q -T 2 --header='Content-Type: application/json' --post-data="$json_body" -O /dev/null "$upload_url" >/dev/null 2>&1 || true
    fi
}

case "$(uname -s 2>/dev/null)" in
    Darwin)
        play_macos || terminal_bell
        ;;
    Linux)
        play_linux || terminal_bell
        ;;
    *)
        terminal_bell
        ;;
esac

status="$(agent_status "$kind")"
send_coding_agent_session_status "$status"

if [ "$kind" = "complete" ] || [ "$kind" = "finished" ]; then
    printf '{"continue":true}\n'
fi

exit 0
