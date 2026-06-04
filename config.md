# Project Configuration & Code Style Guidelines

Tài liệu này quy định các nguyên tắc kiến trúc và quy ước lập trình cho dự án Shoe Store API.

## 1. Kiến trúc hệ thống (System Architecture)
Dự án tuân theo kiến trúc phân lớp (Layered Architecture):
- **Controller Layer**: Tiếp nhận request, validation và trả về response.
- **Service Layer**: Xử lý logic nghiệp vụ (Business Logic).
- **Repository Layer**: Tương tác với cơ sở dữ liệu (Spring Data JPA).
- **DTO Layer**: Chứa các đối tác truyền tải dữ liệu (Request/Response).

## 2. Quy ước đặt tên (Naming Conventions)
- **Entity**: Đặt tên theo mẫu `[Name]Entity`. Ví dụ: `UserEntity`, `ProductEntity`.
- **Repository**: Đặt tên theo mẫu `[Name]Repository`. Ví dụ: `UserRepository`.
- **Service**: Đặt tên theo mẫu `[Name]Service`. Ví dụ: `AuthService`.
- **Controller**: Đặt tên theo mẫu `[Name]Controller`. Ví dụ: `CartController`.
- **DTO**:
    - Request: `[Name]Request`.
    - Response: `[Name]Response`.

## 3. Quy tắc Code (Coding Rules)
- **Sử dụng Lombok**: Tận dụng `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`, `@RequiredArgsConstructor`.
- **Validation**: Sử dụng `@Valid` và các annotation của `jakarta.validation.constraints` trong các Request DTO.
- **API Response**: Luôn đóng gói kết quả trả về trong class `ApiResponse<T>`.
- **Xử lý lỗi**: Sử dụng các custom exception như `BadRequestException`, `NotFoundException` và xử lý tập trung (Global Exception Handler).

## 4. Công nghệ cốt lõi
- **Framework**: Spring Boot.
- **ORM**: Spring Data JPA / Hibernate.
- **Database**: MySQL/PostgreSQL (tùy cấu hình).
- **Language**: Java 17+.

## 5. Lưu ý đặc biệt
- Luôn sử dụng tiếng Việt cho các thông báo lỗi hoặc thông báo thành công trả về cho client qua `ApiResponse`.
- Đảm bảo tính nhất quán giữa các layer.
