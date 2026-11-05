# 🎙️ Adam TTS Studio

**Adam TTS Studio** là ứng dụng web chạy local chuyên nghiệp giúp chuyển đổi kịch bản văn bản thành giọng đọc truyền cảm với ElevenLabs API (giọng đọc huyền thoại **Adam**), quản lý **Pool nhiều API Key** tự động luân chuyển khi hết credit, và xuất file âm thanh chuẩn **WAV 44.1kHz** cùng file phụ đề **SRT khớp từng frame** để nhập trực tiếp vào **CapCut**.

---

## 🌟 Tính năng nổi bật

1. **Quản lý Pool nhiều API Key (Key Pool)**:
   - Dán nhiều key cùng lúc (mỗi key một dòng).
   - Key được lưu tại backend (`./data/keys.json`), **frontend không bao giờ nhận key đầy đủ** (chỉ nhận bản che dạng `sk_a...9b1c`). Không bao giờ log key.
   - Nút "Kiểm tra credit" gọi endpoint `v1/user/subscription` lấy số lượng ký tự còn lại, hạn mức và tier. Nếu key thiếu quyền đọc credit thì vẫn dùng để tạo giọng bình thường.
   - Bảng thống kê hiển thị thanh tiến trình trực quan của từng key, tổng credit khả dụng và số ký tự đã tiêu thụ trong phiên.

2. **Cơ chế chọn key & luân chuyển thông minh**:
   - Dùng key theo thứ tự danh sách, chỉ chọn key còn đủ credit cho độ dài đoạn hiện tại.
   - **Lỗi 429 (Rate limit)**: Tự động đợi và thử lại (backoff 2.5s, 5s, 7.5s tối đa 3 lần), sau đó chuyển sang key kế tiếp.
   - **Lỗi 401/402/403 (Quota exceeded)**: Đánh dấu key hết credit, đặt `remaining = độ dài đoạn - 1` để các đoạn ngắn hơn vẫn có cơ hội tận dụng phần credit lẻ còn lại; sau đó lập tức chuyển sang key tiếp theo.
   - **Hết key khả dụng**: Dừng job nhưng **giữ nguyên toàn bộ các đoạn đã đọc thành công**. Người dùng chỉ cần thêm key mới và bấm nút **"Thêm Key & Tiếp tục"** để chạy tiếp mà không tốn công đọc lại từ đầu.

3. **Soạn thảo kịch bản & Tách đoạn**:
   - Đếm số ký tự, số từ và số đoạn theo thời gian thực.
   - Hai chế độ tách: *Mỗi dòng là một đoạn* hoặc *Đoạn cách nhau bằng dòng trống*.
   - Tự động cắt đoạn dài hơn 4.500 ký tự (v3) hoặc 9.000 ký tự (các model khác) theo ranh giới câu (`.`, `!`, `?`, `…`).
   - Dải nút chèn thẻ cảm xúc tại vị trí con trỏ: `[excited]`, `[whispers]`, `[sad]`, `[angry]`, `[laughs]`, `[sighs]`, `[curious]`, `[short pause]`, `[long pause]`.

4. **Cài đặt giọng đọc & Eleven v3**:
   - Model: `eleven_v3` (mặc định), `eleven_multilingual_v2`, `eleven_flash_v2_5`.
   - Stability: 3 chế độ `Creative (0.0)`, `Natural (0.5 - Mặc định)`, `Robust (1.0)`.
   - Voice ID mặc định: `pNInz6obpgDQGcFmaJgB` (Adam).
   - Tùy chỉnh khoảng nghỉ giữa các đoạn (mặc định `0.4s`).

5. **Xử lý âm thanh & Timeline CapCut chuẩn xác**:
   - Cache từng đoạn theo hash SHA-256 (`model + voice + stability + text`) trong `./data/cache`. Chạy lại cùng kịch bản sẽ lấy từ cache, không gọi API và **không tốn credit**.
   - Đo độ dài chính xác từng đoạn bằng `ffprobe`.
   - Ghép âm thanh bằng `ffmpeg` xuất định dạng **WAV 44.1kHz mono 16-bit** (chuẩn âm thanh dựng phim) và **MP3 128kbps**.
   - Mốc thời gian được tính toán theo mẫu (sample) `44100Hz`, loại bỏ hoàn toàn hiện tượng lệch tích luỹ.
   - Xuất file **SRT** (đã tự động lược bỏ các thẻ `[...]` trong phụ đề), **timeline.json** và **timeline.csv**.
   - Nút **"Đọc lại đoạn này"**: Xoá cache của riêng đoạn đó, gọi API tạo lại và tự động ghép nối lại toàn bộ file âm thanh & SRT.

---

## 🛠️ Yêu cầu hệ thống

- **Java**: JDK 21 trở lên
- **Node.js**: Node 20 trở lên & npm
- **FFmpeg & FFprobe**: Đã cài đặt và có trong biến môi trường `PATH`

### Hướng dẫn cài đặt nhanh trên Windows qua PowerShell (Admin)

Nếu máy bạn chưa có JDK hoặc FFmpeg, có thể cài siêu nhanh bằng `winget`:
```powershell
# Cài đặt JDK 21 (Eclipse Temurin)
winget install EclipseAdoptium.Temurin.21.JDK

# Cài đặt Maven
winget install Apache.Maven

# Cài đặt FFmpeg
winget install Gyan.FFmpeg
```
> *Lưu ý:* Sau khi cài đặt xong, hãy mở lại cửa sổ Terminal hoặc khởi động lại máy để biến môi trường `PATH` có hiệu lực.

---

## 🚀 Hướng dẫn khởi chạy ứng dụng

Ứng dụng gồm 2 phần: Backend (Spring Boot trên cổng 8080) và Frontend (Vite React trên cổng 5173).

### Cách 1: Chạy bằng file lệnh `.bat` (Khuyên dùng trên Windows)
1. Nhấp đúp vào file `start-backend.bat` để chạy backend.
2. Nhấp đúp vào file `start-frontend.bat` để chạy frontend.
3. Trình duyệt sẽ mở tại địa chỉ: `http://localhost:5173`.

---

### Cách 2: Chạy bằng dòng lệnh thủ công

#### Bước 1: Khởi chạy Backend (Java Spring Boot)
Mở cửa sổ dòng lệnh thứ nhất:
```bash
cd backend
mvn spring-boot:run
```
*Backend sẽ chỉ bind vào `127.0.0.1:8080` để đảm bảo an toàn.*

#### Bước 2: Khởi chạy Frontend (React + Vite)
Mở cửa sổ dòng lệnh thứ hai:
```bash
cd frontend
npm install
npm run dev
```
Truy cập `http://localhost:5173` trên trình duyệt. Vite đã được cấu hình proxy mọi request `/api/*` về `127.0.0.1:8080`.

---

## 🔑 Hướng dẫn lấy API Key ElevenLabs

1. Đăng ký hoặc đăng nhập tài khoản tại: [https://elevenlabs.io](https://elevenlabs.io).
2. Bấm vào ảnh đại diện góc dưới bên trái → chọn **Profile + API key**.
3. Bấm biểu tượng con mắt hoặc **Create API Key** để sao chép key.
4. Mở **Adam TTS Studio**, bấm **"Thêm Key"** và dán danh sách key vào ô nhập (mỗi key một dòng), sau đó bấm **"Lưu vào Pool"**.

---

## 🎬 Hướng dẫn nhập file WAV & SRT vào CapCut

Để giọng đọc Adam và phụ đề chữ hiển thị khớp từng khung hình 100%:

1. **Nhập âm thanh**:
   - Sau khi tạo xong, bấm nút **"Tải WAV"** trên màn hình kết quả.
   - Trong giao diện CapCut PC, vào mục **Phương tiện (Media)** → bấm **Nhập (Import)** hoặc kéo trực tiếp file `adam_tts_*.wav` xuống thanh **Timeline Âm thanh (Audio track)** bắt đầu tại giây `00:00`.
2. **Nhập phụ đề tự động**:
   - Bấm nút **"Tải SRT"** từ ứng dụng.
   - Trên thanh menu của CapCut, bấm vào tab **Văn bản (Text)** → chọn **Phụ đề cục bộ (Local Captions)**.
   - Bấm nút **Nhập (Import)** và chọn file `adam_tts_*.srt`.
   - Kéo dải phụ đề xuống thanh Timeline ngay phía trên đường âm thanh.
3. **Tuỳ biến**:
   - Phụ đề đã được căn chỉnh mốc thời gian hoàn hảo và loại bỏ mọi thẻ biểu cảm thô. Bạn có thể chọn phong cách chữ, màu sắc, viền và hiệu ứng chạy chữ tuỳ ý trong CapCut!

---

## 📂 Cấu trúc thư mục

```
MODEL API KEY ADAM/
├── backend/
│   ├── pom.xml                               # Cấu hình Maven, Java 21, Spring Boot 3
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/adamtts/studio/
│   │   │   │   ├── config/WebConfig.java     # CORS strict localhost dev, RestClient
│   │   │   │   ├── controller/               # ApiKeyController, JobController, SystemController
│   │   │   │   ├── dto/                      # Các DTO giao tiếp API
│   │   │   │   ├── exception/                # Bộ xử lý lỗi tiếng Việt
│   │   │   │   ├── model/                    # Entity lưu trữ key, job, segment
│   │   │   │   └── service/
│   │   │   │       ├── ApiKeyService.java         # Quản lý key pool, retry & luân chuyển
│   │   │   │       ├── AudioProcessingService.java # ffmpeg/ffprobe ghép wav/mp3 & cache
│   │   │   │       ├── ElevenLabsClient.java      # Gọi API ElevenLabs RestClient
│   │   │   │       ├── JobManagerService.java     # Quản lý tiến trình async, dừng & tiếp tục
│   │   │   │       ├── TextSplittingService.java  # Tách đoạn, cắt câu, xoá thẻ cảm xúc
│   │   │   │       └── TimelineService.java       # Tính toán sample timeline, xuất SRT/CSV/JSON
│   │   │   └── resources/
│   │   │       └── application.properties    # Cấu hình bind 127.0.0.1:8080, thư mục data
│   │   └── test/                             # Unit tests logic key, cắt câu, timeline SRT
├── frontend/
│   ├── package.json                          # React 18, Vite, TypeScript, Tailwind
│   ├── vite.config.ts                        # Proxy /api sang backend
│   └── src/
│       ├── components/                       # Header, ApiKeyManager, ScriptEditor, VoiceSettings, ResultPlayer...
│       ├── services/api.ts                   # Gọi API backend
│       └── types/index.ts                    # TypeScript interface
├── data/                                     # Thư mục lưu trữ local (ĐÃ ĐƯỢC .gitignore BẢO VỆ)
│   ├── keys.json                             # File lưu API keys (nội bộ backend)
│   ├── cache/                                # Cache file âm thanh theo hash SHA-256
│   └── jobs/                                 # Lưu trữ các job, file wav, srt, timeline
├── start-backend.bat                         # Khởi động nhanh backend
├── start-frontend.bat                        # Khởi động nhanh frontend
└── README.md                                 # Tài liệu hướng dẫn sử dụng
```

---

## 🧪 Chạy Unit Test kiểm thử backend

Các unit test được viết độc lập bằng JUnit 5 và Mockito, mock toàn bộ ElevenLabs client (không gọi API thật và không tốn credit):
```bash
cd backend
mvn test
```
Bộ test bao gồm:
1. `ApiKeyServiceTest`: Kiểm tra che key, nạp nhiều key, luân chuyển key khi gặp lỗi `quota_exceeded`, bỏ qua key thiếu credit, và ném `NoAvailableKeyException` khi hết key.
2. `TextSplittingServiceTest`: Kiểm tra chế độ tách theo dòng, tách theo đoạn trống, cắt câu khi vượt quá 4.500 ký tự (v3) và 9.000 ký tự, lược bỏ thẻ `[...]`.
3. `TimelineServiceTest`: Kiểm tra định dạng `HH:mm:ss,SSS`, tính mốc sample-accurate theo tần số 44.1kHz và sinh file SRT chuẩn.

