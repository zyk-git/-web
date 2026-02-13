# 农村婚礼/乔迁电子礼簿（前后端分离，移动端优先）

## 项目简介
这是一个**极简、个人可用**的微信 H5 电子礼簿系统，解决农村婚礼/乔迁现场礼金登记问题。

- 前端：Vue3 + Vite + JavaScript + Vant3 + qrcode.js
- 后端：Java17 + Spring Boot3 + Spring Data JPA + Spring Web
- 数据库：MySQL8
- 部署：前端 `dist` 放 Nginx 静态目录，后端 JAR 独立运行

## 项目结构
```text
-web/
├─ backend/
│  ├─ pom.xml
│  └─ src/main/
│     ├─ java/com/example/giftbook/
│     │  ├─ Application.java
│     │  ├─ config/CorsConfig.java
│     │  ├─ controller/ApiController.java
│     │  ├─ entity/Activity.java
│     │  ├─ entity/GiftRecord.java
│     │  ├─ repository/ActivityRepository.java
│     │  ├─ repository/GiftRecordRepository.java
│     │  └─ util/{TokenStore,RateLimitStore}.java
│     └─ resources/
│        ├─ application.yml
│        └─ static/uploads/
├─ frontend/
│  ├─ package.json
│  ├─ vite.config.js
│  ├─ index.html
│  └─ src/
│     ├─ main.js
│     ├─ App.vue
│     ├─ router/index.js
│     └─ views/{GuestView.vue,AdminView.vue}
└─ README.md
```

## MySQL 建表 SQL
```sql
CREATE DATABASE IF NOT EXISTS giftbook DEFAULT CHARACTER SET utf8mb4;
USE giftbook;

CREATE TABLE IF NOT EXISTS activity (
  id BIGINT PRIMARY KEY,
  title VARCHAR(200),
  date VARCHAR(50),
  location VARCHAR(255),
  wx_qrcode_path VARCHAR(255),
  ali_qrcode_path VARCHAR(255)
);

INSERT INTO activity(id, title, date, location, wx_qrcode_path, ali_qrcode_path)
VALUES (1, '某某婚礼/乔迁', '2026-01-01', '某某村文化礼堂', '', '')
ON DUPLICATE KEY UPDATE id = id;

CREATE TABLE IF NOT EXISTS gift_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  submit_id VARCHAR(64),
  name VARCHAR(100),
  amount DECIMAL(10,2),
  relation VARCHAR(50),
  blessing VARCHAR(255),
  payer_name VARCHAR(100),
  payer_phone VARCHAR(30),
  submit_time DATETIME,
  ip VARCHAR(64)
);
```

## application.yml 配置模板
```yml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/giftbook?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
  jpa:
    hibernate:
      ddl-auto: update

app:
  cors-origin: "https://yourdomain.com"
```

## 前端开发与打包
```bash
cd frontend
npm install
npm run dev
npm run build
```

## 前端 Nginx 部署（把 dist 整体复制）
1. 打包后得到 `frontend/dist`
2. 复制到 Nginx 静态目录，例如 `/usr/share/nginx/html/giftbook/`
3. Nginx `location /` 指向该目录并开启 history 路由回退

参考配置：
```nginx
server {
  listen 80;
  server_name yourdomain.com;

  root /usr/share/nginx/html/giftbook;
  index index.html;

  location / {
    try_files $uri $uri/ /index.html;
  }
}
```

## 后端启动命令
```bash
cd backend
mvn clean package
java -jar target/giftbook-backend-1.0.0.jar
```

## 默认后台账号密码
- 用户名：`admin`
- 密码：`admin`

## 二维码生成与使用
- 后台 `/admin` 登录后会自动生成宾客页面二维码（内容是 `当前域名/`）
- 可直接截图打印，贴在现场让亲友扫码进入录入
- 微信/支付宝收款码通过后台上传替换

## 说明
- 防刷：同一 IP 1 分钟最多提交 3 次
- 上传图片保存到：`backend/src/main/resources/static/uploads`
- 管理端请求自动携带 token（localStorage）
