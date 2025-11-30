package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.Operator;
import com.hamradio.logbook.repository.OperatorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OperatorService {

    private final OperatorRepository operatorRepository;

    @Autowired
    public OperatorService(OperatorRepository operatorRepository) {
        this.operatorRepository = operatorRepository;
    }

    public Operator createOperator(Operator operator) {
        return operatorRepository.save(operator);
    }

    public Optional<Operator> getOperatorById(Long id) {
        return operatorRepository.findById(id);
    }

    public Optional<Operator> getOperatorByCallsign(String callsign) {
        return operatorRepository.findByCallsign(callsign);
    }

    public List<Operator> getAllOperators() {
        return operatorRepository.findAll();
    }

    public Operator updateOperator(Long id, Operator operatorUpdates) {
        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Operator not found"));

        if (operatorUpdates.getCallsign() != null) {
            operator.setCallsign(operatorUpdates.getCallsign());
        }
        if (operatorUpdates.getName() != null) {
            operator.setName(operatorUpdates.getName());
        }
        if (operatorUpdates.getLicenseClass() != null) {
            operator.setLicenseClass(operatorUpdates.getLicenseClass());
        }
        if (operatorUpdates.getEmail() != null) {
            operator.setEmail(operatorUpdates.getEmail());
        }

        return operatorRepository.save(operator);
    }

    public void deleteOperator(Long id) {
        operatorRepository.deleteById(id);
    }
}
