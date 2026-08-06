# pac4j-yiban

[English](./README.md) | [简体中文](./README.zh-CN.md)

## Table of Contents

- [1. Project Overview](#1-project-overview)
- [2. Features & Status](#2-features--status)
- [3. Requirements & Compatibility](#3-requirements--compatibility)
- [4. Architecture & Modules](#4-architecture--modules)
- [5. Installation](#5-installation)
- [6. Quick Start](#6-quick-start)
- [7. Configuration](#7-configuration)
- [8. Core Usage / API](#8-core-usage--api)
- [9. Testing & Build](#9-testing--build)
- [10. Versioning & Branches](#10-versioning--branches)
- [11. Contributing & License](#11-contributing--license)

## 1. Project Overview

**pac4j-yiban** is a [pac4j](https://www.pac4j.org) 5.0.x extension that adds **YiBan (易班) Light App**
authentication to pac4j. YiBan is a Chinese campus social/education platform; a "light app" runs inside the
YiBan client and receives a signed `verify_request` parameter. This library decrypts that parameter with the
app's `appId`/`appSecret`, exchanges the embedded `access_token` for the real-name profile via the YiBan
OpenAPI, and produces a pac4j profile.

It is built on `io.github.easy4j:pac4j-uniauth` / `io.github.easy4j:pac4j-biz`, which provide the generic
`Token` machinery.

| Is                                                               | Is not                                          |
| :--------------------------------------------------------------- | :---------------------------------------------- |
| YiBan Light App OAuth client for pac4j                           | A full YiBan SDK (posts, forums, etc.)          |
| Decrypts `verify_request`, calls `openapi.yiban.cn/user/real_me` | A replacement for YiBan's official OAuth docs   |
| Produces a profile with `yb_studentid` / `yb_username` mapping   | A general-purpose campus SSO connector          |

Typical scenarios:

| Scenario                       | Description                                                    |
| :----------------------------- | :------------------------------------------------------------- |
| YiBan Light App login          | Apps embedded in the YiBan client authenticate silently        |
| Real-name verification         | `yb_studentid` and `yb_username` from the real-name API        |
| Campus app integration         | Share the profile across school-built applications             |

## 2. Features & Status

| Capability                                                  | Status      | Notes                                                                                    |
| :---------------------------------------------------------- | :---------- | :--------------------------------------------------------------------------------------- |
| `verify_request` AES decryption (`utils.AESDecoder`)        | Implemented | AES/CBC/NoPadding; key = `appSecret`, IV = `appId`                                       |
| Unauthorized-user detection                                 | Implemented | `visit_oauth == "false"` → `CredentialsException`                                        |
| Real-name profile fetch (`real_me` API)                     | Implemented | GET `https://openapi.yiban.cn/user/real_me?access_token=...`                             |
| Profile mapping (`YiBanLightAppTokenProfileDefinition`)     | Implemented | Maps `yb_studentid` → `pid`/`userid`, `yb_username` → `xm`; failure uses `msgCN`         |
| OAuth login URL generation                                  | Implemented | `https://oauth.yiban.cn/code/html?client_id=...&redirect_uri=...`                        |
| Token parameter extraction                                  | Implemented | `YiBanTokenParameterExtractor` (GET/POST checks)                                         |
| Unit tests                                                  | Not present | Only a misplaced placeholder `TokenExample` (in package `org.pac4j.ext.uniauth`)         |

## 3. Requirements & Compatibility

| Requirement | Version                         |
| :---------- | :------------------------------ |
| JDK         | 8+                              |
| Maven       | 3.0+ (wrapper included)         |
| pac4j       | 5.0.x                           |
| pac4j-uniauth | `1.0.x.20260630-SNAPSHOT`     |
| fastjson    | 2.0.x (JSON parsing)            |

Version lines of the easy4j project:

| Branch        | JDK  | Version pattern | Notes                       |
| :------------ | :--- | :-------------- | :-------------------------- |
| `feature/1.0.x` | 8    | `1.0.x.*`       | This README, current branch |
| `feature/2.0.x` | 17   | `2.0.x.*`       | JDK 17 line                 |
| `feature/3.0.x` | 21   | `3.0.x.*`       | JDK 21 line                 |

## 4. Architecture & Modules

```text
 YiBan client -> light app (verify_request)
              |
              v
 YiBanLightAppTokenClient (org.pac4j.yiban)
              |
              v
 YiBanTokenParameterExtractor (verify_request)
              |
              v
 YiBanLightAppTokenAuthenticator
   AES decrypt (appSecret/appId) -> visit_oauth
              |
              v
 real_me API (access_token)
              |
              v
 YiBanLightAppTokenProfile (yb_studentid/yb_username ...)
```

Single-module Maven project (`jar` packaging):

| Package                    | Responsibility                                        |
| :------------------------- | :---------------------------------------------------- |
| `org.pac4j.yiban`          | Client, authenticator, profile, parameter extractor   |
| `org.pac4j.yiban.utils`    | `AESDecoder` (AES/CBC/NoPadding, hex-to-binary)       |

## 5. Installation

Artifacts are published to the aliyun repository and GitHub Releases; they are **not** on Maven Central yet.

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>pac4j-yiban</artifactId>
    <version>1.0.x.20260630-SNAPSHOT</version>
</dependency>
```

```groovy
implementation 'io.github.easy4j:pac4j-yiban:1.0.x.20260630-SNAPSHOT'
```

## 6. Quick Start

```java
String appId = "your-yiban-app-id";
String appSecret = "your-yiban-app-secret";

YiBanLightAppTokenAuthenticator authenticator =
        new YiBanLightAppTokenAuthenticator(appId, appSecret);

YiBanLightAppTokenClient client = new YiBanLightAppTokenClient(appId);
client.setAuthenticator(authenticator);
client.setName("yiban-light-app");
client.setLoginUrl("https://apps.example.edu.cn/yiban/callback");

// Builds: https://oauth.yiban.cn/code/html?client_id={appId}&redirect_uri={encoded callback}
String oauthUrl = client.getLoginUrl();
```

Expected result: after the user authorizes in the YiBan client, the app receives `verify_request`; the
authenticator decrypts it, detects `visit_oauth == "false"` (throws `CredentialsException` for
non-authorized users), otherwise calls the `real_me` API with the `access_token` and builds a
`YiBanLightAppTokenProfile` whose `getId()` returns the student id (`yb_studentid`).

## 7. Configuration

| Setting                        | How                                                              | Default                |
| :----------------------------- | :--------------------------------------------------------------- | :--------------------- |
| App ID                         | `YiBanLightAppTokenClient(String appId)` + authenticator ctor    | — (required)           |
| App Secret                     | `YiBanLightAppTokenAuthenticator(String appId, String appSecret)` | — (required)          |
| Callback (login) URL           | inherited `setLoginUrl(String)`                                  | —                      |
| Token parameter name           | inherited from `TokenClient` (set via `setParameterName`)        | `""` (must be set)     |
| GET request support            | inherited `setSupportGetRequest(boolean)`                        | `false`                |
| POST request support           | inherited `setSupportPostRequest(boolean)`                       | `true`                 |
| Real-name API endpoint         | field `realMe` in the authenticator                              | `https://openapi.yiban.cn/user/real_me` |

## 8. Core Usage / API

`real_me` response contract (as implemented in `YiBanLightAppTokenProfileDefinition.extractUserProfile`):

```json
{
  "status": "success",
  "info": {
    "yb_userid": "7400172",
    "yb_username": "yang",
    "yb_usernick": "yang",
    "yb_sex": "M",
    "yb_studentid": "41364",
    "yb_schoolid": "34270",
    "yb_schoolname": "Hangzhou ... College",
    "yb_realname": "yang",
    "yb_birthday": "1987-12-26"
  }
}
```

Failure response throws `CredentialsException` using the `info.msgCN` field:

```json
{ "status": "fail", "info": { "msgCN": "..." } }
```

Key classes:

| Type                                            | Responsibility                                              |
| :---------------------------------------------- | :---------------------------------------------------------- |
| `YiBanLightAppTokenClient`                      | OAuth login URL + credentials extraction                    |
| `YiBanLightAppTokenAuthenticator`               | Decrypt `verify_request`, call `real_me`, build the profile |
| `YiBanTokenParameterExtractor`                  | Read the token from request parameters (GET/POST rules)     |
| `YiBanLightAppTokenProfileDefinition`           | Map `info` JSON to the profile; student id → `pid`/`userid` |
| `YiBanLightAppTokenProfile`                     | Profile fields: `userid`, `pid`, `xm`, `ptype`, `csrq`, `flag`; `getId()` returns `pid` |
| `utils.AESDecoder`                              | AES/CBC/NoPadding decryption + hex-to-binary                |

## 9. Testing & Build

```bash
./mvnw clean verify
```

- Maven wrapper (`mvnw`) is committed to the repository.
- JaCoCo is configured with a line-coverage rule of 90% (`haltOnFailure=false`).
- No unit tests exist yet (only a misplaced placeholder `TokenExample`); the coverage gate is not effectively
  enforced on this branch (known gap). Tests for the AES decoder and the profile mapping are the most
  valuable additions.

## 10. Versioning & Branches

| Branch        | JDK  | Version pattern | Maintenance                          |
| :------------ | :--- | :-------------- | :----------------------------------- |
| `feature/1.0.x` | 8    | `1.0.x.*`       | Current branch                       |
| `feature/2.0.x` | 17   | `2.0.x.*`       | JDK 17 line                          |
| `feature/3.0.x` | 21   | `3.0.x.*`       | JDK 21 line                          |

Artifacts are distributed via the aliyun Maven repository and GitHub Releases. Use the branch matching your
JDK baseline.

## 11. Contributing & License

Contributions are welcome — especially unit tests for the decrypt/profile-mapping logic. Please open an issue
before larger changes.

This project is licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).
