package com.movie.user_service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.movie.user_service.dto.RespDto;
import com.movie.user_service.dto.SignupDto;
import com.movie.user_service.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "userName", source = "name")
    @Mapping(target = "oauthAccounts", ignore = true)
    @Mapping(target = "roles", ignore = true)
    User fromSignupDto(SignupDto dto);
    
    @Mapping(target = "name", source = "userName")
    RespDto toRespDto(User user);
}
