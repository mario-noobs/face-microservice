package com.mario.backend.face.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaceRegisterRequest {

    @NotBlank(message = "Image data is required")
    @JsonProperty("image_data")
    private String imageData;
}
