# CheckTimeDeploy

Siêu nhẹ - Java 21 - Hỗ trợ 1.21.x - Chủ: **minhhaudev**

Plugin kiểm tra thời gian deploy và uptime cho server Paper/Spigot 1.21.x

## Tính năng
- Ghi log thời gian deploy/start khi enable
- Lệnh `/checktimedeploy` (alias: `/ctd`, `/deploytime`, `/checkdeploy`) hiển thị:
  - Thời gian deploy
  - Thời gian khởi động
  - Thời gian hiện tại
  - Uptime
  - Phiên bản server & số người chơi online
- Không config, không database, không dependency - jar < 10KB

## Yêu cầu
- Java 21
- Paper/Spigot 1.21.x (api-version 1.21)

## Build
```bash
mvn clean package
```
Jar ở `target/check-time-deploy-1.0.0.jar`

## Cài đặt
Copy `check-time-deploy-1.0.0.jar` vào `plugins/` và restart server.

## Lệnh & Quyền
- `/checktimedeploy` - `checktimedeploy.use` (default: true)
- `/checktimedeploy reload` - `checktimedeploy.admin` (default: op)

## Tác giả
**minhhaudev** - Java 21 | 1.21.x
