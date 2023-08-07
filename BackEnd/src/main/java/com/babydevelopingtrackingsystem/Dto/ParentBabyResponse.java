package com.babydevelopingtrackingsystem.Dto;

import com.babydevelopingtrackingsystem.Model.BabyVaccination;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class ParentBabyResponse {
    private int id;
    private String babyName;
    private String midwifeName;
    private String doctorName;

    private String birthday;


    private String gender;
    @JsonProperty
    private List<BabyVaccinationResponse> babyVaccinations;
}