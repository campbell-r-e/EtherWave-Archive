package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.Contest;
import com.hamradio.logbook.repository.ContestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ContestService {

    private final ContestRepository contestRepository;

    @Autowired
    public ContestService(ContestRepository contestRepository) {
        this.contestRepository = contestRepository;
    }

    public List<Contest> getAllContests() {
        return contestRepository.findAll();
    }

    public List<Contest> getActiveContests() {
        return contestRepository.findAll().stream()
                .filter(contest -> Boolean.TRUE.equals(contest.getIsActive()))
                .toList();
    }

    public Optional<Contest> getContestById(Long id) {
        return contestRepository.findById(id);
    }

    public Optional<Contest> getContestByCode(String contestCode) {
        return contestRepository.findByContestCode(contestCode);
    }

    public Contest createContest(Contest contest) {
        return contestRepository.save(contest);
    }

    public Contest updateContest(Long id, Contest contestUpdates) {
        Contest contest = contestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contest not found with id: " + id));

        if (contestUpdates.getContestName() != null) {
            contest.setContestName(contestUpdates.getContestName());
        }
        if (contestUpdates.getDescription() != null) {
            contest.setDescription(contestUpdates.getDescription());
        }
        if (contestUpdates.getStartDate() != null) {
            contest.setStartDate(contestUpdates.getStartDate());
        }
        if (contestUpdates.getEndDate() != null) {
            contest.setEndDate(contestUpdates.getEndDate());
        }
        if (contestUpdates.getIsActive() != null) {
            contest.setIsActive(contestUpdates.getIsActive());
        }
        if (contestUpdates.getValidatorClass() != null) {
            contest.setValidatorClass(contestUpdates.getValidatorClass());
        }
        if (contestUpdates.getRulesConfig() != null) {
            contest.setRulesConfig(contestUpdates.getRulesConfig());
        }

        return contestRepository.save(contest);
    }

    public void deleteContest(Long id) {
        contestRepository.deleteById(id);
    }
}
