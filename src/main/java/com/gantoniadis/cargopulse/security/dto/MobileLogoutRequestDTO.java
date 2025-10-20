package com.gantoniadis.cargopulse.security.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileLogoutRequestDTO {

    private String refreshToken;
}
