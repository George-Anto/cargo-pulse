package com.gantoniadis.cargopulse.security.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequestDTO {

    private String refreshToken;
}
