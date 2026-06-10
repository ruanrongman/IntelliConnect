---
name: intelliconnect-service-style
description: Create or update IntelliConnect Spring Boot service/serviceimpl code in this repository style. Use when adding backend services, service implementations, request DTOs, repository methods, CRUD methods, token-filtered get methods, SafetyService-bound permission boundaries, duplicate checks, dependency checks before delete, or Java compile verification for IntelliConnect. Also use when deciding whether a delete should fail on dependencies or cascade-delete related tables.
---

# IntelliConnect Service Style

Use this skill when implementing backend service/serviceimpl code in IntelliConnect. Prefer `PUT`/update flows that identify the target row by `id`.

## Workflow

1. Read existing nearby examples before editing:
   - `src/main/java/top/rslly/iot/services/agent/*ServiceImpl.java`
   - `src/main/java/top/rslly/iot/services/thingsModel/*ServiceImpl.java`
   - matching `dao/`, `models/`, and `param/request/` files.
2. Prefer the existing repository/service naming style over new abstractions.
3. Add or update the request DTO in `top.rslly.iot.param.request` when the service method accepts input from controller code.
4. Add derived Spring Data repository methods for simple lookups.
5. Keep controller permission checks outside mutating service methods when the repository pattern already uses `SafetyServiceImpl` wrappers.
6. Before implementing any delete path, inspect all related tables/repositories and decide whether the correct behavior is dependency failure or cascade delete.
7. If the dependency policy is not already obvious from nearby code, ask the user whether to return `HAS_DEPENDENCIES` or delete related records too.
8. Compile with JDK 21 after edits:
   ```powershell
   $env:JAVA_HOME='C:\Users\Lenovo\.jdks\ms-21.0.9'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd -q -DskipTests compile
   ```

## Service Interface Pattern

Use interfaces under the same service package as the implementation.

Common method shape:

```java
List<Entity> findAllById(int id);

List<Entity> findAllByProductId(int productId);

JsonResult<?> getThing(String token);

JsonResult<?> postThing(ThingParam param);

JsonResult<?> updateThing(ThingParam param);

JsonResult<?> deleteThing(int id);
```

Use `String token` for list getters that need user-specific filtering. Do not pass token into `post`, `update`, or `delete` when controller/SafetyService will authorize the operation before calling the service.

## Implementation Pattern

Use `@Service`, `@Resource`, `@Transactional(rollbackFor = Exception.class)`, `JsonResult<?>`, `ResultTool`, and `ResultCode` to match the repo.

For `get... (String token)`, keep the whole role-based flow in one method, like other impls:

```java
String tokenDeal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
String role = JwtTokenUtil.getUserRole(tokenDeal);
String username = JwtTokenUtil.getUsername(tokenDeal);
List<Entity> result;
if (role.equals("ROLE_" + "wx_user")) {
  if (wxUserRepository.findAllByName(username).isEmpty()) {
    return ResultTool.fail(ResultCode.COMMON_FAIL);
  }
  // collect records through wx product binds
} else if (!role.equals("[ROLE_admin]")) {
  if (userRepository.findAllByUsername(username).isEmpty()) {
    return ResultTool.fail(ResultCode.COMMON_FAIL);
  }
  // collect records through user product binds
} else {
  result = repository.findAll();
}
if (result.isEmpty()) {
  return ResultTool.fail(ResultCode.COMMON_FAIL);
} else
  return ResultTool.success(result);
```

Do not split this into a helper unless the surrounding codebase already does so for the same type of service. Do not `return null`; return `ResultTool.fail(ResultCode.COMMON_FAIL)`.

## Mutation Rules

For `post`:
- Validate required fields and referenced records such as `productId`.
- If the controller already uses `@Valid`, do not duplicate DTO null/blank checks in the service.
- Normalize strings with `trim()` before duplicate checks and saving.
- Check duplicates before saving.
- Return `ResultTool.fail(ResultCode.PARAM_NOT_VALID)` for missing/invalid parameters.
- Return `ResultTool.fail(ResultCode.COMMON_FAIL)` for duplicate business records unless the local service uses a more specific result code.

For `update`:
- Prefer `id`-based updates for `PUT`: require `id`, load the existing entity first, and fail with `PARAM_NOT_VALID` when missing.
- Do not design new `PUT` flows around natural keys unless the user explicitly asks for that exception.
- Check duplicate conflicts while excluding the current `id`.
- Update only intended fields and save the existing entity.

For `delete`:
- Load by id first; fail with `PARAM_NOT_VALID` when missing.
- Check dependent records before deleting.
- If related tables should block deletion, use `ResultCode.HAS_DEPENDENCIES`.
- If related tables should be removed too, delete them in the same transaction before deleting the parent row.
- When the dependency policy is unclear, ask the user before coding the delete path.
- Return the repository delete result through `ResultTool.success(result)`.

## Duplicate Checks

Use the business key the user specifies, including scope fields such as `productId`.

Example for scoped uniqueness:

```java
List<Entity> duplicateList =
    repository.findAllByProductIdAndPairCodeAndAgentType(productId, pairCode, agentType);
for (Entity duplicate : duplicateList) {
  if (!duplicate.getId().equals(param.getId())) {
    return ResultTool.fail(ResultCode.COMMON_FAIL);
  }
}
```

When adding repository methods, prefer Spring Data derived names:

```java
List<Entity> findAllByProductIdAndPairCodeAndAgentType(Integer productId,
    String pairCode, String agentType);
```

## Request DTO Pattern

Use Lombok getters/setters or `@Data`, and Jakarta validation annotations already used in the repo.

```java
@Getter
@Setter
public class ThingParam {
  private Integer id;

  @NotBlank(message = "name 不能为空")
  @Size(min = 1, max = 255, message = "name 长度必须在 1 到 255 之间")
  private String name;

  @NotNull(message = "productId 不能为空")
  private Integer productId;
}
```

Use `Integer` for optional `id` and nullable request fields that need `@NotNull`.

## Safety Boundary

If controller code will wrap calls with `SafetyServiceImpl`, keep service mutation methods focused on business invariants, not permission checks.

Expected controller shape:

```java
if (!safetyService.controlAuthorizeProduct(header, param.getProductId()))
  return ResultTool.fail(ResultCode.NO_PERMISSION);
return thingService.postThing(param);
```

For delete/update by id, add a `SafetyServiceImpl` authorization helper only when controller integration is part of the task.
