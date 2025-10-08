package rta.service;

import org.springframework.stereotype.Service;
import rta.model.MerchantProfile;
import rta.repository.ProfileRepository;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;

    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public MerchantProfile login(String username, String password) {
        MerchantProfile profile = profileRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!profile.getPassword().equals(password)) {
            throw new RuntimeException("Invalid password");
        }

        return profile;
    }

    public MerchantProfile register(MerchantProfile profile) {
        if (profileRepository.findByUsername(profile.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        return profileRepository.save(profile);
    }

    public MerchantProfile getProfile(String merchantId) {
        return profileRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant profile not found: " + merchantId));
    }

    public MerchantProfile updateProfile(String merchantId, MerchantProfile newProfile) {
        MerchantProfile existing = profileRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant profile not found: " + merchantId));

        existing.setName(newProfile.getName());
        existing.setEmail(newProfile.getEmail());
        existing.setCompany(newProfile.getCompany());
        existing.setContact(newProfile.getContact());
        existing.setAddress(newProfile.getAddress());
        existing.setJoinedOn(newProfile.getJoinedOn());
        return profileRepository.save(existing);
    }
}
