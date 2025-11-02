package perdesatis.demo.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopUpSessionData {
    private String username;
    private String fullName;
    private String phone;
    private String address;
    private String city;
    private String addressDetail;
    private BigDecimal amount;
}
