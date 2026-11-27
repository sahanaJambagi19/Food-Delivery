package com.fooddelivery.service.impl;

import com.fooddelivery.dto.request.*;
import com.fooddelivery.dto.response.*;
import com.fooddelivery.entity.*;
import com.fooddelivery.exception.*;
import com.fooddelivery.repository.*;
import com.fooddelivery.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepo;
    private final AddressRepository addressRepo;
    private final OrderRepository orderRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserProfileResponse getProfile(Long userId) {
        User user = findUser(userId);
        return toProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User user = findUser(userId);
        if (!user.getPhone().equals(req.getPhone()) && userRepo.existsByPhone(req.getPhone()))
            throw new DuplicateException("Phone number already in use");
        user.setName(req.getName());
        user.setPhone(req.getPhone());
        return toProfileResponse(userRepo.save(user));
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest req) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword()))
            throw new BusinessException("Current password is incorrect");
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepo.save(user);
    }

    @Override
    @Transactional
    public AddressResponse addAddress(Long userId, AddressRequest req) {
        User user = findUser(userId);
        if (req.isDefaultAddress()) clearDefaultAddress(userId);
        Address address = Address.builder()
                .user(user)
                .label(req.getLabel() != null ? req.getLabel() : "Home")
                .addressLine(req.getAddressLine())
                .city(req.getCity())
                .state(req.getState())
                .pincode(req.getPincode())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .defaultAddress(req.isDefaultAddress())
                .build();
        return toAddressResponse(addressRepo.save(address));
    }

    @Override
    public List<AddressResponse> getAddresses(Long userId) {
        return addressRepo.findByUserId(userId).stream()
                .map(this::toAddressResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest req) {
        Address address = findAddress(userId, addressId);
        if (req.isDefaultAddress()) clearDefaultAddress(userId);
        address.setLabel(req.getLabel() != null ? req.getLabel() : address.getLabel());
        address.setAddressLine(req.getAddressLine());
        address.setCity(req.getCity());
        address.setState(req.getState());
        address.setPincode(req.getPincode());
        address.setLatitude(req.getLatitude());
        address.setLongitude(req.getLongitude());
        address.setDefaultAddress(req.isDefaultAddress());
        return toAddressResponse(addressRepo.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = findAddress(userId, addressId);
        if (orderRepo.existsByAddressId(addressId))
            throw new BusinessException("This address is linked to an order and cannot be deleted. You can keep it in your address book.");
        addressRepo.delete(address);
    }

    @Override
    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        clearDefaultAddress(userId);
        Address address = findAddress(userId, addressId);
        address.setDefaultAddress(true);
        addressRepo.save(address);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User findUser(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private Address findAddress(Long userId, Long addressId) {
        Address a = addressRepo.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
        if (!a.getUser().getId().equals(userId))
            throw new BusinessException("Address does not belong to this user");
        return a;
    }

    private void clearDefaultAddress(Long userId) {
        addressRepo.findByUserIdAndDefaultAddressTrue(userId).ifPresent(a -> {
            a.setDefaultAddress(false);
            addressRepo.save(a);
        });
    }

    private UserProfileResponse toProfileResponse(User u) {
        return UserProfileResponse.builder()
                .id(u.getId()).name(u.getName()).email(u.getEmail())
                .phone(u.getPhone()).role(u.getRole().getName())
                .profileImage(u.getProfileImage()).active(u.isActive())
                .createdAt(u.getCreatedAt()).build();
    }

    private AddressResponse toAddressResponse(Address a) {
        return AddressResponse.builder()
                .id(a.getId()).label(a.getLabel()).addressLine(a.getAddressLine())
                .city(a.getCity()).state(a.getState()).pincode(a.getPincode())
                .latitude(a.getLatitude()).longitude(a.getLongitude())
                .defaultAddress(a.isDefaultAddress()).build();
    }
}
