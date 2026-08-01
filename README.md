# ZirAddon 🚀 (Được tôi remake lại từ cái addon rác 500.000 VNĐ)
> Phiên bản: 1.21.11

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen.svg)](https://minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.18.2-blue.svg)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**ZirAddon** là một mod addon Fabric cao cấp dành cho **Meteor Client** và **Litematica** (dành cho Minecraft **1.21.11**). Mod được thiết kế chuyên sâu giúp tự động hóa quá trình đặt block theo bản vẽ (schematic), xoay hướng block chính xác (rotation spoofing), tự động cân chỉnh redstone, tự động bán đồ (AutoSell) tích hợp thông báo qua Discord Webhook, và bảo vệ giao diện người dùng.

---

## 📑 Danh Sách Các Module & Logic Hoạt Động

### 1. 🏗️ AutoPlace (`auto-place`)
Module cốt lõi phục vụ tự động xây dựng theo bản vẽ Litematica.
* **Rotation Spoofing (Xoay hướng thông minh)**: Tự động tính toán góc nhìn (Yaw & Pitch) cần thiết cho tất cả các loại block có hướng (Piston, Observer, Dispenser, Dropper, Crafter, Cầu thang, Bẫy sập, Gỗ,...) và gửi gói tin xoay (packet spoof) trong tick đặt block mà không làm giật camera của người chơi.
* **Redstone Auto-Adjust (Cân chỉnh Redstone)**: Tự động phát hiện và tương tác (click chuột phải) để đồng bộ thời gian trễ của Repeater và chế độ (Compare/Subtract) của Comparator theo đúng sơ đồ schematic.
* **Instant Auto-Pick (Đổi đồ siêu tốc)**: Tự động tìm và đổi block cần đặt từ kho đồ (Inventory / Hotbar) lên tay chính ngay lập tức (0-tick delay).
* **Prevent GUI & Safety Mode**: Tự động giả lập trạng thái Sneak khi đặt block lên các khối có giao diện (Chest, Furnace, Shulker Box) để tránh mở GUI nhầm. Tích hợp trễ ngẫu nhiên (Safety Variance) tránh bị phát hiện bởi anti-cheat.
* **Air Place & Support**: Hỗ trợ đặt block nền tạm thời khi vị trí schematic không có khối tiếp xúc.

### 2. 🧭 AutoDirection (`auto-direction`)
* Ép hướng đặt của các block (Piston, Sticky Piston, Dispenser, Dropper, Hopper, Observer) theo hướng cố định do người dùng tùy chọn (`NORTH`, `SOUTH`, `EAST`, `WEST`, `UP`, `DOWN`).

### 3. 💰 AutoSell (`auto-sell`)
System tự động bán vật phẩm chuyên nghiệp:
* **Tương tác Rương & Lệnh**: Tự động chuyển vật phẩm mục tiêu trong rương bán đồ hoặc thực hiện lệnh `/sell all`.
* **Phân tích Chat & Thống kê**: Tự động đọc và phân tích tin nhắn từ server chat để tính chính xác số tiền nhận được (hỗ trợ các định dạng `$`, `k`, `m`, `b` và tiếng Việt/Anh).
* **Discord Webhook Reporting**: Tự động gửi hoặc cập nhật báo cáo trực tiếp về máy chủ Discord qua Webhook (sử dụng phương thức HTTP `PATCH` để cập nhật 1 message duy nhất tránh spam channel). Hiển thị chi tiết: Thời gian chạy, Tên người chơi, IP Server, Tổng doanh thu, Số lần bán.

### 4. 🧩 CrafterSetup (`crafter-setup`)
* Cung cấp giao diện lưới 3x3 tương tác trực quan để tự động vô hiệu hóa/bật các ô trong khối **Crafter** (Minecraft 1.21), phục vụ tự động chế tạo theo công thức cố định.

### 5. 🥷 FakeSneak (`fake-sneak`)
* Tự động gửi trạng thái cúi người (Sneak) khi người chơi tương tác với danh sách block tùy chọn (Chest, Anvil, Hopper, Shulker Box...) để dễ dàng đặt khối lên trên chúng mà không bị mở màn hình GUI.

### 6. 🚪 GuiClose (`gui-close`)
* Tự động đóng các màn hình giao diện rương/kho đồ sau một khoảng thời gian trễ chỉ định (có tùy chọn bỏ qua giao diện Chat và Meteor Client).

### 7. 🎮 DiscordRPC (`discord-rpc`)
* Tích hợp trạng thái Discord Rich Presence hiển thị thông tin máy chủ đang chơi, tên nhân vật và phiên bản ZirAddon.

---

## 🛠️ Kiến Trúc Kỹ Thuật (Architecture & Mixins)

Dự án sử dụng SpongePowered Mixin để can thiệp vào luồng xử lý của Minecraft Client:

* **`ClientPlayerInteractionManagerMixin`**:
  * Can thiệp vào `interactBlock` để hủy hoặc hoãn các tương tác thủ công khi AutoPlace/AutoDirection đang hoạt động.
  * Tính toán góc xoay thông qua `RotationCalculator`, đưa vào hàng đợi `RotationSpoofer`.
  * Thực hiện tráo đổi vật phẩm kho đồ tức thì qua `PlayerInventoryAccessor`.
* **`ClientPlayerEntityMixin`**:
  * Can thiệp vào `sendMovementPackets` để ghi đè Yaw/Pitch chính xác trong gói tin gửi tới server.
  * Tạm dừng di chuyển của người chơi khi đang trong khoảng thời gian trễ (`placeDelayMs`).
* **`MinecraftClientMixin`**:
  * Bổ sung luồng Raytrace đọc bản vẽ Litematica (`SchematicWorldHandler`) khi mục tiêu tâm ngắm của người chơi không trúng khối thực tế.
* **`EasyPlaceUtilsMixin` & `WorldUtilsMixin`**:
  * Ghi đè phương thức restriction check của Litematica (`placementRestrictionInEffect`), cho phép AutoPlace hoạt động mượt mà không bị giới hạn gốc của Litematica.
* **`ChatHudMixin`**:
  * Đọc sự kiện nhận tin nhắn chat `addMessage` để phục vụ parser doanh thu của AutoSell.

---

## ⚙️ Cấu Hình (Configuration)

File cấu hình mod được lưu trữ tự động tại:
`.minecraft/config/ziraddon-addon.json`

Có thể tùy chỉnh trực tiếp thông qua giao diện màn hình ModConfig (tích hợp **ModMenu**).

---

## 📦 Hướng Dẫn Build

### Yêu cầu môi trường:
* **Java Development Kit (JDK)**: Version 21 trở lên
* **Gradle**: 8.x (hoặc sử dụng Gradle Wrapper)

### Các bước biên dịch:
1. Clone repository về máy:
   ```bash
   git clone https://github.com/your-username/ZirAddon.git
   cd ZirAddon
   ```
2. Thực hiện lệnh build JAR:
   ```bash
   ./gradlew build
   ```
3. File mod biên dịch sẽ nằm tại:
   `build/libs/ZirAddon-1.3.6-R67.jar`

---

## 📜 Giấy Phép (License)

Dự án được phân phối theo giấy phép [MIT License](LICENSE).
