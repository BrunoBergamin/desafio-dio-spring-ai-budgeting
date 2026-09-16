package dio.budgeting.mapper;

import dio.budgeting.dto.response.UserResponse;
import dio.budgeting.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone());
    }
}
