package com.arena.app.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.arena.app.models.ContinueLearning;
import com.arena.app.models.Problem;
import com.arena.app.models.User;
import com.arena.app.repository.ProblemRepository;
import com.arena.app.repository.UserRepository;

import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final LiveData<User> user;
    private final LiveData<Problem> dailyChallenge;
    private final LiveData<List<ContinueLearning>> continueLearningList;

    public HomeViewModel(@NonNull Application application) {
        super(application);

        UserRepository userRepository = new UserRepository(application);
        ProblemRepository problemRepository = new ProblemRepository(application);

        user = userRepository.getUserStats();
        dailyChallenge = problemRepository.getDailyChallenge();
        continueLearningList = problemRepository.getContinueLearningList();
    }

    public LiveData<User> getUser() { return user; }
    public LiveData<Problem> getDailyChallenge() { return dailyChallenge; }
    public LiveData<List<ContinueLearning>> getContinueLearningList() { return continueLearningList; }
}
