package com.indium.iplassignment.service;

import com.indium.iplassignment.entity.*;
import com.indium.iplassignment.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MatchService {

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private OfficialRepository officialRepository;

    @Autowired
    private InningRepository inningRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private OutcomeRepository outcomeRepository;

    // Clear all caches when new data is uploaded
    @Transactional
    @CacheEvict(value = {"matchesByPlayer", "wicketsByPlayer", "cumulativeScoreByPlayer", "inningScoresByDate", "playersByTeamAndMatch", "matchRefereesByMatchNumber", "topBatsmen", "strikeRate", "topWicketTakers"}, allEntries = true)
    public boolean saveMatchData(JsonNode matchData) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // Parse the Match object first
            Match match = parseMatch(matchData);

            // Check if the match number already exists in the database
            Optional<Match> existingMatch = matchRepository.findById(match.getMatchNumber());

            // If the match exists, return false
            if (existingMatch.isPresent()) {
                log.warn("Match with match number {} already exists in the database.", match.getMatchNumber());
                return false; // Indicate that the match was not saved
            }

            // If match doesn't exist, proceed to save
            log.info("Saving new match with match number {}", match.getMatchNumber());
            matchRepository.save(match);

            // Parse and save Teams and Players
            parseTeamsAndPlayers(matchData.get("info").get("players"), match);

            // Parse and save Officials
            parseOfficials(matchData.get("info").get("officials"), match);

            // Parse and save Innings and Deliveries
            parseInnings(matchData.get("innings"), match);

            // Parse and save Outcome
            parseOutcome(matchData.get("info").get("outcome"), match);

            return true; // Indicate that the match was saved successfully
        } catch (Exception e) {
            log.error("Failed to save match data for match number {}", matchData.get("info").get("event").get("match_number"), e);
            throw new RuntimeException("Failed to save match data", e);
        }
    }

    private Match parseMatch(JsonNode matchData) {
        Match match = new Match();
        JsonNode info = matchData.get("info");

        try {
            match.setMatchNumber((long) info.get("event").get("match_number").asInt()); // match_number is the key
            match.setMatchType(info.get("match_type").asText());
            match.setCity(info.get("city").asText());
            match.setVenue(info.get("venue").asText());
            match.setEventName(info.get("event").get("name").asText());
            match.setWinner(info.get("outcome").get("winner").asText());
            match.setTossWinner(info.get("toss").get("winner").asText());
            match.setTossDecision(info.get("toss").get("decision").asText());
            match.setPlayerOfMatch(info.get("player_of_match").get(0).asText());
            match.setMatchDate(LocalDate.parse(info.get("dates").get(0).asText())); // Using match_date from "dates" array
        } catch (Exception e) {
            log.error("Error parsing match details from JSON", e);
            throw new RuntimeException("Error parsing match data", e);
        }

        return match;
    }

    private void parseTeamsAndPlayers(JsonNode playersData, Match match) {
        if (playersData == null || !playersData.isObject()) {
            throw new IllegalArgumentException("Invalid or missing players data");
        }

        // Prepare lists for batch saving
        List<Team> teamsToSave = new ArrayList<>();
        List<Player> playersToSave = new ArrayList<>();

        // Iterate over each team
        playersData.fields().forEachRemaining(entry -> {
            String teamName = entry.getKey();
            JsonNode players = entry.getValue();

            if (players == null || !players.isArray()) {
                throw new IllegalArgumentException("Players data must be an array for team: " + teamName);
            }

            // Check if the team already exists
            Optional<Team> existingTeam = teamRepository.findByTeamName(teamName);
            Team team;
            if (existingTeam.isPresent()) {
                team = existingTeam.get();
            } else {
                team = new Team();
                team.setTeamName(teamName);
                teamsToSave.add(team); // Collect team for batch save
            }

            // Save each player to the team
            players.forEach(playerName -> {
                if (playerName.isTextual()) {
                    Player player = new Player();
                    player.setPlayerName(playerName.asText());
                    player.setTeam(team);
                    playersToSave.add(player); // Collect player for batch save
                } else {
                    throw new IllegalArgumentException("Invalid player name format for team: " + teamName);
                }
            });
        });

        // Batch save teams and players
        if (!teamsToSave.isEmpty()) {
            teamRepository.saveAll(teamsToSave);
        }
        if (!playersToSave.isEmpty()) {
            playerRepository.saveAll(playersToSave);
        }
    }


    private void parseOfficials(JsonNode officialsData, Match match) {
        List<Official> officialsToSave = new ArrayList<>();

        try {
            officialsData.fields().forEachRemaining(entry -> {
                String role = entry.getKey();
                JsonNode officials = entry.getValue();

                officials.forEach(officialName -> {
                    Official official = new Official();
                    official.setOfficialName(officialName.asText());
                    official.setRole(role);
                    official.setMatch(match);
                    officialsToSave.add(official); // Collect official for batch save
                });
            });

            // Batch save
            if (!officialsToSave.isEmpty()) {
                officialRepository.saveAll(officialsToSave);
            }
        } catch (Exception e) {
            log.error("Error parsing officials for match number {}", match.getMatchNumber(), e);
            throw new RuntimeException("Error parsing officials", e);
        }
    }


    private void parseInnings(JsonNode inningsData, Match match) {
        if (inningsData == null || !inningsData.isArray()) {
            log.warn("No innings data found for match number {}", match.getMatchNumber());
            return;
        }
        Map<String, Team> teamCache = new HashMap<>();

        try {
            // Pre-fetch teams for better performance
            inningsData.forEach(inningNode -> {
                String teamName = inningNode.get("team").asText();
                teamCache.computeIfAbsent(teamName, name ->
                        teamRepository.findByTeamName(name).orElseThrow(() -> new RuntimeException("Team not found: " + name))
                );
            });

            inningsData.forEach(inningNode -> {
                String teamName = inningNode.get("team").asText();
                Team team = teamCache.get(teamName);

                Inning inning = new Inning();
                inning.setMatch(match);
                inning.setTeam(team);
                inningRepository.save(inning);

                parseDeliveries(inningNode.get("overs"), inning);
            });
        } catch (Exception e) {
            log.error("Error parsing innings and deliveries for match number {}", match.getMatchNumber(), e);
            throw new RuntimeException("Error parsing innings and deliveries", e);
        }
    }

    private void parseDeliveries(JsonNode oversData, Inning inning) {
        List<Delivery> deliveriesToSave = new ArrayList<>();

        oversData.forEach(overNode -> {
            int overNumber = overNode.get("over").asInt();

            int ballNumber = 1;
            for (JsonNode deliveryNode : overNode.get("deliveries")) {
                Delivery delivery = new Delivery();
                delivery.setInning(inning);
                delivery.setOverNumber(overNumber);
                delivery.setBallNumber(ballNumber);
                delivery.setBatter(deliveryNode.get("batter").asText());
                delivery.setBowler(deliveryNode.get("bowler").asText());
                delivery.setNonStriker(deliveryNode.get("non_striker").asText());
                delivery.setRunsBatter(deliveryNode.get("runs").get("batter").asInt());
                delivery.setRunsExtras(deliveryNode.get("runs").get("extras").asInt());
                delivery.setRunsTotal(deliveryNode.get("runs").get("total").asInt());

                // Check for wicket details
                if (deliveryNode.has("wickets")) {
                    JsonNode wicketNode = deliveryNode.get("wickets").get(0);
                    delivery.setWicket(true);
                    delivery.setDismissalKind(wicketNode.get("kind").asText());
                    delivery.setPlayerOut(wicketNode.get("player_out").asText());

                    if (wicketNode.has("fielders")) {
                        List<String> fielderNames = new ArrayList<>();
                        wicketNode.get("fielders").forEach(fielderNode ->
                                fielderNames.add(fielderNode.get("name").asText())
                        );
                        delivery.setFielders(String.join(", ", fielderNames));
                    }
                } else {
                    delivery.setWicket(false);
                }

                deliveriesToSave.add(delivery);
                ballNumber++;
            }
        });

        if (!deliveriesToSave.isEmpty()) {
            deliveryRepository.saveAll(deliveriesToSave); // Batch save deliveries
        }
    }

    private void parseOutcome(JsonNode outcomeData, Match match) {
        if (outcomeData == null || !outcomeData.has("winner")) {
            log.warn("No outcome data found for match number {}", match.getMatchNumber());
            return;
        }

        try {
            Outcome outcome = new Outcome();
            outcome.setMatch(match);

            // Set the winner team information
            String winnerTeamName = outcomeData.get("winner").asText();
            outcome.setWinnerTeamName(winnerTeamName);

            // Fetch team entity by name
            Optional<Team> winnerTeamOpt = teamRepository.findByTeamName(winnerTeamName);
            winnerTeamOpt.ifPresent(outcome::setWinnerTeam);  // Set the team if present

            if (outcomeData.has("by")) {
                JsonNode byNode = outcomeData.get("by");
                outcome.setOutcomeBy(byNode.has("runs") ? "runs" : "wickets");
                outcome.setMargin(byNode.has("runs") ? byNode.get("runs").asInt() : byNode.get("wickets").asInt());
            }

            outcomeRepository.save(outcome);
        } catch (Exception e) {
            log.error("Error parsing outcome for match number {}", match.getMatchNumber(), e);
            throw new RuntimeException("Error parsing outcome", e);
        }
    }

    // Caching for different methods

    @Cacheable(value = "matchesByPlayer", key = "#playerName")
    public List<Map<String, Object>> getMatchEventsByPlayerName(String playerName) {
        List<Object[]> matchData = matchRepository.findMatchEventsByPlayerName(playerName);

        return matchData.stream().map(data -> {
            Map<String, Object> matchMap = new HashMap<>();
            if (data.length > 0) matchMap.put("match_number", data[0]); // Use match_number instead of match_id
            if (data.length > 1) matchMap.put("match_type", data[1]);
            if (data.length > 2) matchMap.put("city", data[2]);
            if (data.length > 3) matchMap.put("venue", data[3]);
            if (data.length > 4) matchMap.put("event_name", data[4]);
            if (data.length > 5) matchMap.put("winner", data[5]);
            if (data.length > 6) matchMap.put("toss_winner", data[6]);
            if (data.length > 7) matchMap.put("toss_decision", data[7]);
            if (data.length > 8) matchMap.put("player_of_match", data[8]);
            if (data.length > 9) matchMap.put("match_date", data[9]); // Use match_date instead of created_date

            return matchMap;
        }).collect(Collectors.toList());
    }

    @Cacheable(value = "wicketsByPlayer", key = "#playerName")
    public List<Map<String, Object>> getWicketsByBowler(String playerName) {
        List<Object[]> deliveryData = deliveryRepository.findWicketsByBowler(playerName);

        return deliveryData.stream().map(data -> {
            Map<String, Object> wicketMap = new HashMap<>();
            wicketMap.put("deliveryId", data[0]);
            wicketMap.put("over", data[1]);
            wicketMap.put("ball", data[2]);
            wicketMap.put("wicket", data[3]);
            wicketMap.put("batsman", data[4]);
            wicketMap.put("bowler", data[5]);
            wicketMap.put("dismissalType", data[6]);
            return wicketMap;
        }).collect(Collectors.toList());
    }

    @Cacheable(value = "cumulativeScoreByPlayer", unless = "#result == null")
    public Integer getCumulativeScoreByBatter(String playerName) {
        return deliveryRepository.getCumulativeScoreByBatter(playerName);
    }

    @Cacheable(value = "inningScoresByDate", key = "#matchDate")
    public List<Map<String, Object>> getInningScoresByDate(LocalDate matchDate) {
        List<Object[]> scoresData = deliveryRepository.findInningScoresByDate(matchDate);

        return scoresData.stream().map(data -> {
            Map<String, Object> scoreMap = new HashMap<>();
            scoreMap.put("matchNumber", data[0]); // Use match_number
            scoreMap.put("inningId", data[1]);
            scoreMap.put("battingTeam", data[2]);
            scoreMap.put("totalScore", data[3]);
            return scoreMap;
        }).collect(Collectors.toList());
    }

    @Cacheable(value = "playersByTeamAndMatch", key = "{#teamName, #matchNumber}")
    public List<Player> getPlayersByTeamAndMatch(String teamName, int matchNumber) {
        return playerRepository.findPlayersByTeamAndMatch(teamName, matchNumber);
    }

    @Cacheable(value = "matchRefereesByMatchNumber", key = "#matchNumber")
    public List<Official> getMatchRefereesByMatchNumber(Long matchNumber) {
        return officialRepository.findMatchRefereesByMatchNumber(matchNumber);
    }

    @Cacheable(value = "topBatsmen", key = "{#pageable.pageNumber, #pageable.pageSize}")
    public List<Object[]> getTopBatsmen(Pageable pageable) {
        return deliveryRepository.findTopBatsmen(pageable);
    }

    @Cacheable(value = "strikeRate", key = "{#batterName, #matchNumber}")
    public String getStrikeRateByBatterAndMatch(String batterName, int matchNumber) {
        Double strikeRate = deliveryRepository.getStrikeRateByBatterAndMatch(batterName, matchNumber);

        if (strikeRate != null && strikeRate > 0) {
            return "Strike rate for " + batterName + " in match number " + matchNumber + " is " + String.format("%.2f", strikeRate);
        } else {
            return "No data found or strike rate is 0 for the given player and match.";
        }
    }

    @Cacheable(value = "topWicketTakers", key = "{#page, #size}")
    public List<Object[]> getTopWicketTakers(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Object[]> wicketTakersPage = deliveryRepository.findTopWicketTakers(pageRequest);
        return wicketTakersPage.getContent();
    }
}
