package com.nexushub.identity.persistence;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
  UserRow findByUsername(@Param("username") String username);

  UserRow findById(@Param("id") long id);

  List<UserRow> search(@Param("keyword") String keyword, @Param("status") String status);

  long nextId();

  int insert(UserRow row);

  int updateStatus(
      @Param("id") long id,
      @Param("status") String status,
      @Param("expectedRowVersion") int expectedRowVersion);

  int resetPassword(
      @Param("id") long id,
      @Param("passwordHash") String passwordHash,
      @Param("expectedRowVersion") int expectedRowVersion);

  int countEnabledAdmins();
}
