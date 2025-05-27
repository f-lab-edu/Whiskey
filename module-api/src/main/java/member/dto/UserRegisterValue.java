package member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRegisterValue(
    @NotBlank @Email
    String email,
    @NotBlank
    String password,
    @NotBlank
    String userName) {

}
