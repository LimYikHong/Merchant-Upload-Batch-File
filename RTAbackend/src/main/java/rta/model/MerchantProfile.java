package rta.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "profiles")
@Data
public class MerchantProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false, unique = true)
    private String merchantId;

    @Column(nullable = false)
    private String name;

    private String address;

    private String phone;

    private String email;

    private String password;

    private String username;

    private String company;

    private String contact;

    private LocalDateTime joinedOn;

    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;
}
