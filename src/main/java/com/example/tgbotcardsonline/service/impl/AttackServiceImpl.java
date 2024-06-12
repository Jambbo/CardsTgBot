package com.example.tgbotcardsonline.service.impl;

import com.example.tgbotcardsonline.model.Attack;
import com.example.tgbotcardsonline.repository.AttackRepository;
import com.example.tgbotcardsonline.service.AttackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AttackServiceImpl implements AttackService {
    private final AttackRepository attackRepository;

    public Attack createAttack(){
        Attack attack = Attack.builder()

                .build();

        return attack;
    }

}
