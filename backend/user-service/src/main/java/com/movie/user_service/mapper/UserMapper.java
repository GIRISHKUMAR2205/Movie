package com.movie.user_service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.movie.user_service.dto.UserDto;
import com.movie.user_service.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(UserDto userDto);
    UserDto toUserDto(User user);
}
