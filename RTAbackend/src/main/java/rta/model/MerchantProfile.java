package rta.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "merchant_profile")
public class MerchantProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id")
    private String merchantId;

    private String name;
    private String email;
    private String company;
    private String contact;
    private String address;
    private String joinedOn;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "password")
    private String password;
}
