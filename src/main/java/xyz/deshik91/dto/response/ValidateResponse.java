package xyz.deshik91.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateResponse {
    private String email;
    private boolean valid;
    private String tokenType;  // access или refresh
}