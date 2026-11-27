package com.fooddelivery.service;

import com.fooddelivery.dto.request.*;
import com.fooddelivery.dto.response.*;
import java.util.List;

public interface UserService {
    UserProfileResponse getProfile(Long userId);
    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);

    AddressResponse addAddress(Long userId, AddressRequest request);
    List<AddressResponse> getAddresses(Long userId);
    AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request);
    void deleteAddress(Long userId, Long addressId);
    void setDefaultAddress(Long userId, Long addressId);
}
