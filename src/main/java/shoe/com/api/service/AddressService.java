package shoe.com.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shoe.com.api.dto.request.AddressRequest;
import shoe.com.api.entity.AddressEntity;
import shoe.com.api.entity.UserEntity;
import shoe.com.api.exception.BadRequestException;
import shoe.com.api.exception.ResourceNotFoundException;
import shoe.com.api.repository.AddressRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private static final int MAX_ADDRESSES = 10;

    private final AddressRepository addressRepository;

    public List<AddressEntity> getMyAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
    }

    @Transactional
    public AddressEntity create(UserEntity user, AddressRequest request) {
        long count = addressRepository.countByUserId(user.getId());
        if (count >= MAX_ADDRESSES) {
            throw new BadRequestException("Bạn chỉ có thể lưu tối đa " + MAX_ADDRESSES + " địa chỉ");
        }
        // Địa chỉ đầu tiên luôn là mặc định
        boolean makeDefault = count == 0 || Boolean.TRUE.equals(request.getIsDefault());
        if (makeDefault) {
            addressRepository.clearDefault(user.getId());
        }
        AddressEntity address = AddressEntity.builder().user(user).build();
        apply(address, request);
        address.setIsDefault(makeDefault);
        return addressRepository.save(address);
    }

    @Transactional
    public AddressEntity update(Long userId, Long id, AddressRequest request) {
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefault(userId);
        }
        AddressEntity address = findOwned(userId, id);
        apply(address, request);
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            address.setIsDefault(true);
        }
        return addressRepository.save(address);
    }

    @Transactional
    public AddressEntity setDefault(Long userId, Long id) {
        addressRepository.clearDefault(userId);
        AddressEntity address = findOwned(userId, id);
        address.setIsDefault(true);
        return addressRepository.save(address);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        AddressEntity address = findOwned(userId, id);
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        addressRepository.delete(address);
        addressRepository.flush();
        if (wasDefault) {
            // Chuyển mặc định sang địa chỉ mới nhất còn lại
            addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId).stream().findFirst()
                    .ifPresent(a -> {
                        a.setIsDefault(true);
                        addressRepository.save(a);
                    });
        }
    }

    private AddressEntity findOwned(Long userId, Long id) {
        return addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ", "id", id));
    }

    private void apply(AddressEntity address, AddressRequest request) {
        address.setFullName(request.getFullName().trim());
        address.setPhoneNumber(request.getPhoneNumber().trim());
        address.setProvince(request.getProvince().trim());
        address.setWard(request.getWard().trim());
        address.setAddressDetail(request.getAddressDetail().trim());
    }
}
