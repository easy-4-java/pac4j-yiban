# pac4j-yiban

[English](./README.md) | [简体中文](./README.zh-CN.md)

[![Java](https://img.shields.io/badge/Java-17-orange)](https://github.com/easy-4-java/pac4j-yiban) [![License](https://img.shields.io/badge/license-Apache%202.0-green)](./LICENSE)

pac4j-yiban 是 pac4j 5.0.x 的扩展组件，为 pac4j 增加易班轻应用认证能力。

## 目录

- [1. 项目概述](#1-项目概述)
- [2. 功能与状态](#2-功能与状态)
- [3. 环境要求与兼容性](#3-环境要求与兼容性)
- [4. 架构与模块](#4-架构与模块)
- [5. 安装](#5-安装)
- [6. 快速开始](#6-快速开始)
- [7. 配置](#7-配置)
- [8. 核心用法 / API](#8-核心用法--api)
- [9. 测试与构建](#9-测试与构建)
- [10. 版本线与分支](#10-版本线与分支)
- [11. 贡献与许可](#11-贡献与许可)

## 1. 项目概述

**pac4j-yiban** 是 [pac4j](https://www.pac4j.org) 5.0.x 的扩展组件，为 pac4j 增加**易班轻应用**认证能力。易班是国内校园社交/教育平台；轻应用运行在易班客户端内，会收到带签名的 `verify_request` 参数。本组件使用应用的 `appId` / `appSecret` 解密该参数，用其中携带的 `access_token` 调用易班开放平台实名接口获取用户实名信息，并生成 pac4j 用户资料。

它构建在 `io.github.easy4j:pac4j-uniauth` / `io.github.easy4j:pac4j-biz` 之上，后者提供通用的 `Token` 机制。

| 是                                                               | 不是                                          |
| :--------------------------------------------------------------- | :-------------------------------------------- |
| 面向 pac4j 的易班轻应用 OAuth 客户端                               | 完整的易班 SDK（发帖、社区等）                  |
| 解密 `verify_request` 并调用 `openapi.yiban.cn/user/real_me`      | 易班官方 OAuth 文档的替代品                    |
| 将 `yb_studentid` / `yb_username` 映射为用户资料                   | 通用的校园统一认证连接器                        |

典型场景：

| 场景                 | 说明                                                    |
| :------------------- | :------------------------------------------------------ |
| 易班轻应用登录        | 嵌入易班客户端的应用静默完成认证                         |
| 实名信息获取         | 通过实名接口获得 `yb_studentid` 与 `yb_username`         |
| 校园应用集成         | 在多个校内应用间共享该用户资料                           |

## 2. 功能与状态

| 能力                                                      | 状态       | 说明                                                                                    |
| :-------------------------------------------------------- | :--------- | :-------------------------------------------------------------------------------------- |
| `verify_request` AES 解密（`utils.AESDecoder`）            | 已实现     | AES/CBC/NoPadding；key = `appSecret`，IV = `appId`                                       |
| 未授权用户识别                                            | 已实现     | `visit_oauth == "false"` → 抛出 `CredentialsException`                                  |
| 实名资料获取（`real_me` 接口）                             | 已实现     | GET `https://openapi.yiban.cn/user/real_me?access_token=...`                            |
| 资料映射（`YiBanLightAppTokenProfileDefinition`）          | 已实现     | `yb_studentid` → `pid`/`userid`，`yb_username` → `xm`；失败时使用 `msgCN`                |
| OAuth 登录地址生成                                        | 已实现     | `https://oauth.yiban.cn/code/html?client_id=...&redirect_uri=...`                       |
| 令牌参数提取                                              | 已实现     | `YiBanTokenParameterExtractor`（GET/POST 规则检查）                                      |
| 单元测试                                                  | 暂无       | 仅有放错包位置的占位类 `TokenExample`（位于 `org.pac4j.ext.uniauth`）                    |

## 3. 环境要求与兼容性

| 要求           | 版本                           |
| :------------- | :----------------------------- |
| JDK            | 8+                             |
| Maven          | 3.0+（已内置 wrapper）          |
| pac4j          | 5.0.x                          |
| pac4j-uniauth  | `2.0.x.x.20260630-SNAPSHOT`      |
| fastjson       | 2.0.x（JSON 解析）              |

easy4j 项目的版本线：

| 分支           | JDK  | 版本模式   | 说明                            |
| :------------- | :--- | :--------- | :------------------------------ |
| `feature/1.0.x` | 8    | `1.0.x.*`  | 本文档对应分支                   |
| `feature/2.0.x` | 17   | `2.0.x.*`  | JDK 17 版本线                   |
| `feature/3.0.x` | 21   | `3.0.x.*`  | JDK 21 版本线                   |

## 4. 架构与模块

```text
 易班客户端 -> 轻应用 (verify_request)
              |
              v
 YiBanLightAppTokenClient (org.pac4j.yiban)
              |
              v
 YiBanTokenParameterExtractor (verify_request)
              |
              v
 YiBanLightAppTokenAuthenticator
   AES 解密 (appSecret/appId) -> visit_oauth
              |
              v
 real_me 接口 (access_token)
              |
              v
 YiBanLightAppTokenProfile (yb_studentid/yb_username ...)
```

单模块 Maven 项目（`jar` 打包）：

| 包                         | 职责                                              |
| :------------------------- | :------------------------------------------------ |
| `org.pac4j.yiban`          | 客户端、认证器、资料、参数提取器                   |
| `org.pac4j.yiban.utils`    | `AESDecoder`（AES/CBC/NoPadding，十六进制转二进制）|

## 5. 安装

制品发布在阿里云私服与 GitHub Releases，**尚未发布到 Maven Central**。

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>pac4j-yiban</artifactId>
    <version>2.0.x.x.20260630-SNAPSHOT</version>
</dependency>
```

```groovy
implementation 'io.github.easy4j:pac4j-yiban:2.0.x.x.20260630-SNAPSHOT'
```

## 6. 快速开始

```java
String appId = "your-yiban-app-id";
String appSecret = "your-yiban-app-secret";

YiBanLightAppTokenAuthenticator authenticator =
        new YiBanLightAppTokenAuthenticator(appId, appSecret);

YiBanLightAppTokenClient client = new YiBanLightAppTokenClient(appId);
client.setAuthenticator(authenticator);
client.setName("yiban-light-app");
client.setLoginUrl("https://apps.example.edu.cn/yiban/callback");

// 生成：https://oauth.yiban.cn/code/html?client_id={appId}&redirect_uri={编码后的 callback}
String oauthUrl = client.getLoginUrl();
```

预期结果：用户在易班客户端内授权后，应用收到 `verify_request`；认证器先解密该参数，若 `visit_oauth == "false"` 则抛出 `CredentialsException`（未授权用户）；否则使用 `access_token` 调用 `real_me` 接口，构建 `YiBanLightAppTokenProfile`，其 `getId()` 返回学工号（`yb_studentid`）。

## 7. 配置

| 配置项                | 设置方式                                                            | 默认值                      |
| :-------------------- | :------------------------------------------------------------------ | :-------------------------- |
| App ID                | `YiBanLightAppTokenClient(String appId)` 及认证器构造方法            | —（必填）                   |
| App Secret            | `YiBanLightAppTokenAuthenticator(String appId, String appSecret)`   | —（必填）                   |
| 回调（登录）地址       | 继承的 `setLoginUrl(String)`                                        | —                           |
| 令牌参数名             | 继承自 `TokenClient`（通过 `setParameterName` 设置）                 | `""`（需自行设置）          |
| 支持 GET 请求          | 继承的 `setSupportGetRequest(boolean)`                              | `false`                     |
| 支持 POST 请求         | 继承的 `setSupportPostRequest(boolean)`                             | `true`                      |
| 实名接口地址           | 认证器中的 `realMe` 字段                                            | `https://openapi.yiban.cn/user/real_me` |

## 8. 核心用法 / API

`real_me` 响应契约（按 `YiBanLightAppTokenProfileDefinition.extractUserProfile` 的实现）：

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

失败响应使用 `info.msgCN` 抛出 `CredentialsException`：

```json
{ "status": "fail", "info": { "msgCN": "..." } }
```

关键类：

| 类型                                            | 职责                                                        |
| :---------------------------------------------- | :---------------------------------------------------------- |
| `YiBanLightAppTokenClient`                      | OAuth 登录地址生成 + 凭证提取                                |
| `YiBanLightAppTokenAuthenticator`               | 解密 `verify_request`、调用 `real_me`、构建用户资料          |
| `YiBanTokenParameterExtractor`                  | 从请求参数读取令牌（GET/POST 规则）                          |
| `YiBanLightAppTokenProfileDefinition`           | 将 `info` JSON 映射为用户资料；学工号 → `pid`/`userid`       |
| `YiBanLightAppTokenProfile`                     | 资料字段：`userid`、`pid`、`xm`、`ptype`、`csrq`、`flag`；`getId()` 返回 `pid` |
| `utils.AESDecoder`                              | AES/CBC/NoPadding 解密 + 十六进制转二进制                    |

## 9. 测试与构建

```bash
./mvnw clean verify
```

- 仓库内置 Maven wrapper（`mvnw`）。
- 已配置 JaCoCo 行覆盖率 90% 门禁（`haltOnFailure=false`）。
- 目前没有单元测试（仅有放错位置的占位类 `TokenExample`），覆盖率门禁在本分支实际上未被有效执行（已知缺口）。AES 解密与资料映射的测试是最有价值的补充。

## 10. 版本线与分支

| 分支           | JDK  | 版本模式   | 维护说明                          |
| :------------- | :--- | :--------- | :-------------------------------- |
| `feature/1.0.x` | 8    | `1.0.x.*`  | 当前分支                          |
| `feature/2.0.x` | 17   | `2.0.x.*`  | JDK 17 版本线                     |
| `feature/3.0.x` | 21   | `3.0.x.*`  | JDK 21 版本线                     |

制品通过阿里云 Maven 私服与 GitHub Releases 分发。请按 JDK 基线选择对应分支。

## 11. 贡献与许可

欢迎贡献——尤其是解密与资料映射逻辑的单元测试。较大改动请先提交 issue 讨论。

本项目基于 [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0) 许可。
