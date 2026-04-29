# blaze-query-connector-notion

## Integration Summary

### What this connector does

Provides SQL query access to a Notion workspace via the Notion public REST API (v1).
It is security- and compliance-oriented: every queryable type exposes ownership,
lifecycle state (archived, trashed), and content fields relevant to access reviews,
data-loss prevention (DLP), and retention policy enforcement.

Write operations (create page, update page, append blocks) are also available
directly on `NotionClient` for callers that need to mutate workspace content.

---

### Setup

**Dependency**

```gradle
api project(':blaze-query-connector-notion')
```

**Gradle module name:** `:blaze-query-connector-notion`
**Directory:** `connector/notion/`

**QueryContext wiring**

```java
NotionClient client = new NotionClient("secret_YOUR_INTEGRATION_TOKEN");

QueryContext ctx = Queries.createQueryContextBuilder()
    .loadServices()
    .with(NotionConnectorConfig.NOTION_CLIENT, client)
    .build();
```

`NotionClient` takes a single Notion internal integration token (starts with
`secret_`). It wraps `java.net.HttpClient` — no third-party HTTP library dependency.

**Schema object aliases** (register once per `QueryContext`)

```java
builder.registerSchemaObjectAlias(NotionWorkspace.class,    "NotionWorkspace");
builder.registerSchemaObjectAlias(NotionUser.class,         "NotionUser");
builder.registerSchemaObjectAlias(NotionPage.class,         "NotionPage");
builder.registerSchemaObjectAlias(NotionDatabase.class,     "NotionDatabase");
builder.registerSchemaObjectAlias(NotionBlock.class,        "NotionBlock");
builder.registerSchemaObjectAlias(NotionComment.class,      "NotionComment");
builder.registerSchemaObjectAlias(NotionDatabaseRow.class,  "NotionDatabaseRow");
```

---

### Configuration properties (`NotionConnectorConfig`)

| Property constant | Property name | Type | Default | Purpose |
|---|---|---|---|---|
| `NOTION_CLIENT` | `notionClient` | `NotionClient` | — | **Required.** The authenticated API client. |
| `BLOCK_MAX_DEPTH` | `notionBlockMaxDepth` | `Integer` | `1` | How many levels deep to recurse into block children. `1` = direct page children only. |
| `DATABASE_ROWS_ENABLED` | `notionDatabaseRowsEnabled` | `Boolean` | `false` | Must be `true` to activate `NotionDatabaseRowDataFetcher`. Off by default due to cost. |

Multi-workspace setups are supported — configure multiple `NotionClient` instances
via `DataFetcherConfig.getAll()` and each will contribute its own rows.

---

### Queryable types

#### `NotionWorkspace`
**API:** `GET /v1/users/me` — one row per configured `NotionClient`.

| Field | Type | Notes |
|---|---|---|
| `workspaceId` | `String` | UUID of the workspace |
| `workspaceName` | `String` | Display name |
| `botId` | `String` | UUID of the integration bot user |
| `botName` | `String` | Display name of the bot |
| `botAvatarUrl` | `String` | Nullable |
| `maxFileUploadSize` | `String` | e.g. `"5mb"`, nullable |

**Security focus:** Confirms which workspace each integration token is scoped to.
Useful in multi-workspace environments to detect token mix-ups. Also identifies
the bot UUID, which should be excluded from "human-authored content" audit queries.

---

#### `NotionUser`
**API:** `GET /v1/users` — all workspace members (persons and bots). Guests are excluded by the API.

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | UUID |
| `type` | `String` | `"person"` or `"bot"` |
| `name` | `String` | Display name |
| `avatarUrl` | `String` | Nullable |
| `email` | `String` | Nullable; persons only; requires *Read user information (incl. email)* capability |
| `botOwnerId` | `String` | Nullable; bots with `botOwnerType = "user"` only |
| `botOwnerType` | `String` | Nullable; `"workspace"` or `"user"` |

**Security focus:**
- Cross-reference `email` against your IdP to find deprovisioned accounts that still have workspace access.
- `type = 'bot'` rows are integrations — audit `botOwnerType` to distinguish workspace-level integrations from personal ones.
- Joining `NotionPage.lastEditedById` → `NotionUser.id` reveals pages last touched by users no longer in the workspace.

---

#### `NotionPage`
**API:** `POST /v1/search` (filter `object=page`) — all pages the integration can access.

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | UUID |
| `createdTime` | `String` | ISO 8601 |
| `lastEditedTime` | `String` | ISO 8601 |
| `createdById` | `String` | User UUID |
| `lastEditedById` | `String` | User UUID |
| `archived` | `boolean` | Archived via UI or API |
| `inTrash` | `boolean` | In workspace trash (soft-deleted, recoverable) |
| `locked` | `boolean` | Locked pages cannot be edited from the UI |
| `publicUrl` | `String` | **Non-null = page is published to the open internet** via "Share to web" |
| `parentType` | `String` | `"workspace"`, `"page_id"`, `"database_id"`, `"block_id"` |
| `parentId` | `String` | Nullable; UUID of the parent resource |
| `url` | `String` | Internal Notion URL (requires login) |

**Security focus:**
- `publicUrl IS NOT NULL` → page is world-readable. High-priority finding.
- `parentType = 'workspace'` → top-level pages, potentially broadly shared.
- `locked = true` tracks content freeze controls.
- `inTrash = true` pages still contain their content and are API-accessible until permanently deleted — relevant for retention obligations.

---

#### `NotionDatabase`
**API:** `POST /v1/search` (filter `object=database`).

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | UUID |
| `title` | `String` | Plain-text title, nullable if untitled |
| `createdTime` | `String` | ISO 8601 |
| `lastEditedTime` | `String` | ISO 8601 |
| `createdById` | `String` | User UUID |
| `lastEditedById` | `String` | User UUID |
| `archived` | `boolean` | |
| `inTrash` | `boolean` | |
| `parentType` | `String` | `"workspace"`, `"page_id"`, `"block_id"` |
| `parentId` | `String` | Nullable |
| `url` | `String` | |
| `inline` | `boolean` | Inline databases are embedded in pages and less visible in navigation |

**Security focus:**
- `inline = true` databases are hidden inside page content — easy to overlook during access reviews.
- Archived/trashed databases still hold their rows via the API until permanently deleted.

---

#### `NotionBlock`
**API:** `GET /v1/blocks/{pageId}/children` — block children of every accessible page.

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | UUID |
| `pageId` | `String` | UUID of the containing page |
| `type` | `String` | `"paragraph"`, `"heading_1"`, `"image"`, `"toggle"`, etc. |
| `createdTime` | `String` | ISO 8601 |
| `lastEditedTime` | `String` | ISO 8601 |
| `createdById` | `String` | User UUID |
| `lastEditedById` | `String` | User UUID |
| `hasChildren` | `boolean` | Whether nested blocks exist beneath this one |
| `inTrash` | `boolean` | |
| `plainText` | `String` | Nullable; concatenated text from `rich_text` fields for text-type blocks |

`plainText` is populated for: `paragraph`, `heading_1–4`, `bulleted_list_item`,
`numbered_list_item`, `quote`, `callout`, `code`, `toggle`, `to_do`.
It is `null` for structural and media block types (`image`, `divider`, `column_list`, etc.).

**Security focus:**
- `plainText LIKE '%password%'` etc. for DLP scanning of page content.
- Default depth is 1 (direct page children). Set `notionBlockMaxDepth > 1` to scan nested toggle blocks, columns, and synced blocks — each additional depth level multiplies API calls by the number of blocks with `hasChildren = true`.

---

#### `NotionComment`
**API:** `GET /v1/comments?block_id={pageId}` — all comments on every accessible page.

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | UUID |
| `pageId` | `String` | UUID of the containing page |
| `blockId` | `String` | Nullable; UUID of the specific block if block-anchored |
| `discussionId` | `String` | UUID of the discussion thread |
| `createdTime` | `String` | ISO 8601 |
| `lastEditedTime` | `String` | ISO 8601 |
| `createdById` | `String` | User UUID |
| `plainText` | `String` | Nullable; concatenated comment text |

**Security focus:**
- Comments are a high-risk DLP surface — credentials, tokens, and internal URLs frequently appear here and are invisible to page-content scans.
- Comment timestamps and author IDs are preserved even after surrounding page content is edited or deleted, making them useful for forensic audit trails.

---

#### `NotionDatabaseRow`
**API:** `POST /v1/databases/{databaseId}/query` — rows from every accessible database.
**Opt-in:** must set `notionDatabaseRowsEnabled = true`.

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | UUID (same as the underlying page UUID) |
| `databaseId` | `String` | UUID of the parent database |
| `createdTime` | `String` | ISO 8601 |
| `lastEditedTime` | `String` | ISO 8601 |
| `createdById` | `String` | User UUID |
| `lastEditedById` | `String` | User UUID |
| `archived` | `boolean` | |
| `inTrash` | `boolean` | |
| `title` | `String` | Value of the database's title property; primary row identifier |
| `propertiesPlainText` | `String` | All property values as `"Name: value; "` pairs |

`propertiesPlainText` covers these Notion property types: `title`, `rich_text`,
`email`, `phone_number`, `url`, `number`, `checkbox`, `select`, `multi_select`,
`date`, `people`, `status`. Relation, formula, rollup, and file properties are
omitted (not extractable as plain text).

**Security focus:**
- Notion databases are commonly used for HR records, vendor lists, customer contacts, and incident trackers — all high-PII surfaces.
- `propertiesPlainText LIKE '%SSN%'` etc. provides row-level DLP coverage.
- Rows in archived/trashed databases remain accessible until permanently deleted.

---

### Write operations (`NotionClient`)

These are direct methods on `NotionClient`, not DataFetchers:

| Method | HTTP | Purpose |
|---|---|---|
| `createPage(JsonNode body)` | `POST /v1/pages` | Create a new page under a page, database, or workspace |
| `updatePage(String pageId, JsonNode body)` | `PATCH /v1/pages/{id}` | Update properties, archive, trash, lock, or apply a template |
| `appendBlocks(String blockId, List<JsonNode> children)` | `PATCH /v1/blocks/{id}/children` | Append up to 100 block children to a page or block |

Request bodies are plain Jackson `JsonNode` — no generated model classes.

---

### API surface limitations

The Notion **public REST API** does not expose:

| Feature | Where it lives instead |
|---|---|
| SAML SSO configuration | Notion admin UI (Enterprise) |
| SCIM user/group provisioning | SCIM endpoint (Enterprise) |
| Audit logs | Security & Compliance partner programme (Discovery API) |
| Domain verification settings | Notion admin UI (Enterprise) |
| Guest user enumeration | Not available via public API |
| Page permission/ACL details | Not available via public API |

If audit logs or org-level security settings are required, the project must apply
to Notion's Security & Compliance partner programme separately.

---

### Performance and API call budget

| Fetcher | Calls per execution |
|---|---|
| `NotionWorkspaceDataFetcher` | 1 per client |
| `NotionUserDataFetcher` | 1+ per client (paginated) |
| `NotionPageDataFetcher` | 1+ per client (paginated search) |
| `NotionDatabaseDataFetcher` | 1+ per client (paginated search) |
| `NotionBlockDataFetcher` | 1+ per page × depth levels |
| `NotionCommentDataFetcher` | 1+ per page |
| `NotionDatabaseRowDataFetcher` | 1+ per database (paginated) |

In large workspaces, `NotionBlockDataFetcher` (especially at depth > 1),
`NotionCommentDataFetcher`, and `NotionDatabaseRowDataFetcher` can issue hundreds
of API calls. Limit the pages and databases the integration can access in the
Notion UI to control the scope of each query session.

Notion enforces rate limits (HTTP 429). The current `NotionClient` does not
implement automatic retry/back-off — callers should handle `DataFetcherException`
wrapping an `IOException` with status 429.

---

### Notable development decisions

- **No generated API client.** Unlike the Kandji or Jira connectors, there is no
  official Notion Java SDK, so `NotionClient` is a thin hand-written wrapper around
  `java.net.HttpClient`. Jackson is the only added dependency.
- **`fromJson` factory + package-private constructor.** POJOs use a static
  `fromJson(JsonNode)` factory for production deserialization. The constructor is
  package-private so test classes in the same package can construct fixtures directly
  without reflection — the same pattern used by the Datadog connector.
- **`NotionDatabaseRow` is opt-in.** Row fetching is disabled by default and guarded
  by `NotionConnectorConfig.DATABASE_ROWS_ENABLED`. This prevents accidental
  full-workspace scans in production query sessions where only structural metadata
  is needed.
- **Block depth is configurable but conservative by default.** Depth 1 is safe for
  most compliance use cases. Increasing depth beyond 2–3 in a workspace with many
  toggle-heavy pages can easily exhaust the Notion API rate limit.
