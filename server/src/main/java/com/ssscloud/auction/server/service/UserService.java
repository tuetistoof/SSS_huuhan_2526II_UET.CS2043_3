package com.ssscloud.auction.server.service;

import com.ssscloud.auction.common.dto.request.LoginRequest;
import com.ssscloud.auction.common.dto.request.RegisterRequest;
import com.ssscloud.auction.common.dto.response.UserDTO;
import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.model.Bidder;
import com.ssscloud.auction.common.model.Seller;
import com.ssscloud.auction.common.model.base.User;
import com.ssscloud.auction.server.dao.UserDAO;

/*
    * UserService chịu trách nhiệm xử lý logic liên quan đến người dùng, bao gồm:
    * - Đăng nhập
    * - Đăng ký
    * Nó sẽ tương tác với UserDAO để truy xuất và lưu trữ dữ liệu người dùng trong database.
    * Các phương thức sẽ thực hiện các bước sau:
    * 1. validateLoginRequest / validateRegisterRequest: Kiểm tra tính hợp lệ của dữ liệu đầu vào, đảm bảo rằng tất cả các trường cần thiết đều có giá trị hợp lệ.
    * 2. login: Tìm kiếm người dùng theo username, kiểm tra mật khẩu, và trả về UserDTO nếu đăng nhập thành công. Nếu có lỗi, sẽ ném ra IllegalArgumentException với thông báo lỗi cụ thể.
    * 3. register: Kiểm tra xem username và email đã tồn tại chưa, xây dựng đối tượng User mới dựa trên role, lưu trữ vào database, và trả về UserDTO. Nếu có lỗi, sẽ ném ra IllegalArgumentException hoặc RuntimeException với thông báo lỗi cụ thể.
 */
public class UserService {
    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public UserDTO login(LoginRequest req){
        validateLoginRequest(req);
        User user = userDAO.findByUsername(req.getUsername());

        if (user == null)
            throw new IllegalArgumentException("Không tìm thấy user với username: " + req.getUsername());
        if (!user.getPassword().equals(req.getPassword()))
            throw new IllegalArgumentException("Sai mật khẩu");
        return toDTO(user);
    }

    public UserDTO register(RegisterRequest req){
        validateRegisterRequest(req);
        if (userDAO.findByUsername(req.getUsername()) != null)
            throw new IllegalArgumentException("Username đã tồn tại: " + req.getUsername());

        if (userDAO.findByEmail(req.getEmail()) != null)
            throw new IllegalArgumentException("Email đã tồn tại: " + req.getEmail());

        User user = buildUser(req);
        persistUser(user, req.getRole());
        return toDTO(user);
    }


    private void validateLoginRequest(LoginRequest req){
        if (req == null)
            throw new IllegalArgumentException("LoginRequest không được null");
        if (req.getUsername() == null || req.getUsername().isBlank())
            throw new IllegalArgumentException("Username không được null hoặc rỗng");
        if (req.getPassword() == null || req.getPassword().isBlank())
            throw new IllegalArgumentException("Password không được null hoặc rỗng");
    }
    private void validateRegisterRequest(RegisterRequest req){
        if (req == null)
            throw new IllegalArgumentException("RegisterRequest không được null");
        if (req.getUsername() == null || req.getUsername().isBlank())
            throw new IllegalArgumentException("Username không được null hoặc rỗng");
        if (req.getPassword() == null || req.getPassword().isBlank())
            throw new IllegalArgumentException("Password không được null hoặc rỗng");
        if (req.getEmail() == null || req.getEmail().isBlank())
            throw new IllegalArgumentException("Email không được null hoặc rỗng");
        if (req.getRole() == null)
            throw new IllegalArgumentException("Role không được null");

        if (req.getUsername().length() < 3 || req.getUsername().length() > 20)
            throw new IllegalArgumentException("Username phải có độ dài từ 3 đến 20 ký tự");
        if (req.getPassword().length() < 6 || req.getPassword().length() > 100)
            throw new IllegalArgumentException("Password phải có độ dài từ 6 đến 100 ký tự");
        if (!req.getEmail().contains("@")) 
            throw new IllegalArgumentException("Email không hợp lệ");
    }

    private User buildUser(RegisterRequest req){
        return switch (req.getRole()) {
            case BIDDER -> new Bidder(req.getName(), req.getUsername(), req.getPassword(), req.getEmail(), req.getRole());
            case SELLER -> new Seller(req.getName(), req.getUsername(), req.getPassword(), req.getEmail(), req.getRole());
            default -> throw new IllegalArgumentException("Role không hợp lệ: " + req.getRole());
        };
    }
    private void persistUser(User user, UserRole role){
        boolean saved = switch (role) {
            case BIDDER -> userDAO.saveBidder((Bidder) user);
            case SELLER -> userDAO.saveSeller((Seller) user);
            default -> false;
        };

        if (!saved)
            throw new RuntimeException("Lỗi khi lưu user vào database");
    }

    private UserDTO toDTO(User user){
        return new UserDTO(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) { super(message); }
    }
 
    public static class RegistrationException extends RuntimeException {
        public RegistrationException(String message) { super(message); }
    }
}
