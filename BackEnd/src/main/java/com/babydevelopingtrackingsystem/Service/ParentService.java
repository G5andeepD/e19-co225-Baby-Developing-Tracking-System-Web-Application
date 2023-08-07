package com.babydevelopingtrackingsystem.Service;

import com.babydevelopingtrackingsystem.Dto.*;
import com.babydevelopingtrackingsystem.Model.*;
import com.babydevelopingtrackingsystem.Repository.*;
import com.babydevelopingtrackingsystem.Utill.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ParentService {
    private final ParentRepository parentRepository;
    private final BabyRepository babyRepository;

    private final VaccinationRepository vaccinationRepository;

    private final BabyVaccinationRepository babyVaccinationRepository;

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final MidwifeRepository midwifeRepository;

    private final NotificationService notificationService;

    private final BabyHeightWeightRepository babyHeightWeightRepository;

    private final UserRepository userRepository;
    public ParentService(ParentRepository parentRepository, BabyRepository babyRepository, VaccinationRepository vaccinationRepository, BabyVaccinationRepository babyVaccinationRepository, AppointmentRepository appointmentRepository, DoctorRepository doctorRepository, MidwifeRepository midwifeRepository, NotificationService notificationService, BabyHeightWeightRepository babyHeightWeightRepository, UserRepository userRepository) {
        this.parentRepository = parentRepository;
        this.babyRepository = babyRepository;
        this.vaccinationRepository = vaccinationRepository;
        this.babyVaccinationRepository = babyVaccinationRepository;
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.midwifeRepository = midwifeRepository;
        this.notificationService = notificationService;
        this.babyHeightWeightRepository = babyHeightWeightRepository;
        this.userRepository = userRepository;
    }
    //Baby
    public boolean doesBabyExistForParent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        Parent parent = parentRepository.findByEmail(email);

        return babyRepository.existsByParent(parent);
    }

    public void addNewBaby(BabyRegistrationRequest babyRegistrationRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        Parent parent = parentRepository.findByEmail(email);
        Baby baby = new Baby();

        baby.setParent(parent);
        String birthday = babyRegistrationRequest.getBirthday().toString();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        baby.setDateofBirth(String.valueOf(LocalDate.parse(birthday, formatter)));
        baby.setName(babyRegistrationRequest.getFirstName()+" "+ babyRegistrationRequest.getLastName());
        baby.setGender(babyRegistrationRequest.getGender());
        baby.setBloodType(babyRegistrationRequest.getBloodType());
        baby.setBirthLength(babyRegistrationRequest.getBirthHeight());
        baby.setBirthWeight(babyRegistrationRequest.getBirthWeight());
        baby.setEyeColor(babyRegistrationRequest.getEyeColor());
        baby.setHairColor(babyRegistrationRequest.getHairColor());
        baby.setSkinColor(babyRegistrationRequest.getSkinColor());
        baby.setNationality(babyRegistrationRequest.getNationality());
        //baby.setMotherName(babyRegistrationRequest.getMotherName());
        //baby.setMotherContact(babyRegistrationRequest.getMotherContact());
        //baby.setFatherName(babyRegistrationRequest.getFatherName());
        //baby.setFatherContact(babyRegistrationRequest.getFatherContact());
        baby.setAllergies(babyRegistrationRequest.getAllergies());
        baby.setImmunizationRecords(babyRegistrationRequest.getImmunizationRecords());
        baby.setGrowthRecords(babyRegistrationRequest.getGrowthRecords());
        baby.setDevelopmentalMilestones(babyRegistrationRequest.getDevelopmentMilestones());




        Baby savedBaby = babyRepository.save(baby);

        //Assign the Compulsory Vaccines to the Baby

        List<Vaccination> compulsoryVaccinations = vaccinationRepository.findByType("Compulsory");

        for (Vaccination vaccination : compulsoryVaccinations) {
            LocalDate dueDate = LocalDate.parse(birthday, formatter).plusMonths(vaccination.getAgeInMonths());
            babyVaccinationRepository.save(new BabyVaccination( dueDate, "Pending",savedBaby, vaccination));
        }



    }

    public ParentBabyResponse getYourBaby() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        Parent parent = parentRepository.findByEmail(email);

        Baby baby =  babyRepository.findBabyByParent(parent);

        ParentBabyResponse babySend = new ParentBabyResponse();
        if (baby!=null){
            List<BabyVaccinationResponse> babyVaccinationResponses = new ArrayList<>();
            List<BabyVaccination> babyVaccinations = baby.getBabyVaccinations();
            for(BabyVaccination babyVaccination:babyVaccinations){
                babyVaccinationResponses.add(new

                        BabyVaccinationResponse(
                                babyVaccination.getId(),
                                babyVaccination.getVaccination().getName(),
                                babyVaccination.getVaccinationDate(),
                                babyVaccination.getStatus()));
            }
            String doctorName;
            String midwifeName;

            try{
                doctorName = baby.getDoctor().getFirstname()+" "+baby.getDoctor().getLastname() ;
            }
            catch(Exception e){
                doctorName = "Not yet Assigned";
            }
            try{
                midwifeName = baby.getMidwife().getFirstname()+" "+baby.getMidwife().getLastname();
            }
            catch(Exception e){
                midwifeName = "Not yet Assigned";
            }
            babySend.setBabyName(baby.getName());
            babySend.setBabyVaccinations(babyVaccinationResponses);
            babySend.setGender(baby.getGender());
            babySend.setDoctorName(doctorName);
            babySend.setMidwifeName(midwifeName);
            babySend.setBirthday(baby.getDateofBirth());
        }


        return babySend;

    }

    public void createAppointment(AppointmentRequest appointmentRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        Parent parent = parentRepository.findByEmail(email);
        Baby baby = babyRepository.findBabyByParent(parent);
        Optional<User> parentUser = userRepository.findById(parent.getId());

        Appointment appointment = new Appointment();
        appointment.setRequestorUser(parentUser.get());

        Role acceptorRole = appointmentRequest.getRole();
        if(acceptorRole.equals(Role.DOCTOR)){
            Optional<User> doctorUser = userRepository.findById(baby.getDoctor().getId());
            appointment.setAcceptorUser(doctorUser.get());
            notificationService.createNotification(doctorUser.get(),"Your Parent " + parentUser.get().getFirstname() + " has requested an Appointment" +
                    "at " + appointmentRequest.getDateTime().toString());

        }
        else if(acceptorRole.equals(Role.MIDWIFE)){
            Optional<User> midwifeUser = userRepository.findById(baby.getMidwife().getId());
            appointment.setAcceptorUser(midwifeUser.get());
            notificationService.createNotification(midwifeUser.get(),"Your Parent " + parentUser.get().getFirstname()+ "has requested an Appointment" +
                    "at " + appointmentRequest.getDateTime().toString());

        }
        appointment.setAppointmentStatus("PENDING");
        appointment.setVenue(appointmentRequest.getVenue());
        appointment.setPlacementDateTime(LocalDateTime.now());
        appointment.setScheduledDateTime(appointmentRequest.getDateTime());

        appointmentRepository.save(appointment);




    }


    //Height and Weight
    public void addHeightWeightRecord(HeightWeightDto heightWeightDto) {
        //Find out who is the parent
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        Parent parent = parentRepository.findByEmail(email);

        //Find out baby based on parent
        Baby baby = babyRepository.findBabyByParent(parent);

        //Build the BabyHeightWeight object using Dto and baby
        BabyHeightWeight babyHeightWeight = new BabyHeightWeight();
        babyHeightWeight.setWeight(heightWeightDto.getWeight());
        babyHeightWeight.setHeight(heightWeightDto.getHeight());
        babyHeightWeight.setDate(heightWeightDto.getDate());

        babyHeightWeight.setBaby(baby);

        //Save to repository
        babyHeightWeightRepository.save(babyHeightWeight);

    }


    //Appointments
    //TODO
}
