# TeamSystem

Language: 中文 | [English](README_en.md)

> 一个支持多服协同的队伍系统

## 目录


- [安装](#安装)
- [配置](#配置)
- [开发](#开发)
- [注意](#注意)

## 安装

得到插件的主体 `TeamSystem-xxx.jar` 后，请将其放入服务器的 `plugins/` 文件夹内

在使用插件之前，您必须安装以下依赖

- [MemoriesOfTime-GameCore](https://github.com/MemoriesOfTime/MemoriesOfTime-GameCore)

- FullKotlinLib 模块
  - 在 GameCore 插件内的 `modules.txt` 内配置  `implementation(cn.lanink.module:FullKotlinLib:1.0.0)` 即可自动下载
  - 初次下载会花费一些时间，请确保下载完毕再安装 TeamSystem 插件本体
  - 您也可以在其他地方获得此模块的 jar 包，将其放入 `plugins/`(GameCore 版本低于 1.6.1 或者 使用 PM1E 核心情况下) 或者将其放入 GameCore 插件目录里的 `modules/` 下(GameCore 1.6.1 及以上) 

- FormDSL 模块

## 配置

- config.yml
  - 多个服务器配置同一个 Mysql 服务器即可开启多服协同

## 开发

待插件 API 稳定后更新

## 注意

插件目前处于快速更新状态，不保证 API 的兼容性