package rta.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import rta.model.MerchantProfile;
import rta.repository.ProfileRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

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

    public MerchantProfile uploadProfilePhoto(String merchantId, MultipartFile file) {
        MerchantProfile profile = getProfile(merchantId);

        if (file.isEmpty()) {
            throw new RuntimeException("Failed to store empty file.");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString() + fileExtension;

            Path uploadPath = Paths.get("uploads/profile-photos");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath);

            String fileUrl = "/uploads/profile-photos/" + newFilename;
            profile.setProfilePhotoUrl(fileUrl);

            return profileRepository.save(profile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }
}
