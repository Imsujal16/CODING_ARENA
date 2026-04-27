package com.arena.app.ui.solver;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.arena.app.R;
import com.arena.app.models.CodeLanguage;
import com.arena.app.models.CompilerSubmissionResultResponse;
import com.arena.app.models.Problem;
import com.arena.app.repository.CompilerRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ProblemSolverFragment extends Fragment {

    private static final String ARG_PROBLEM_ID = "problem_id";
    private static final String ARG_PROBLEM_NUMBER = "problem_number";
    private static final String ARG_PROBLEM_TITLE = "problem_title";
    private static final String ARG_PROBLEM_SLUG = "problem_slug";
    private static final String ARG_PROBLEM_URL = "problem_url";
    private static final String ARG_PROBLEM_DESCRIPTION = "problem_description";
    private static final String ARG_PROBLEM_DIFFICULTY = "problem_difficulty";
    private static final String ARG_PROBLEM_TOPIC = "problem_topic";
    private static final String ARG_PROBLEM_XP = "problem_xp";

    private CompilerRepository compilerRepository;
    private Problem currentProblem;
    private CodeLanguage selectedLanguage = CodeLanguage.JAVA;
    private String lastTemplateApplied;

    private Spinner spinnerLanguage;
    private TextInputEditText inputCode;
    private TextInputEditText inputStdin;
    private TextView textStatus;
    private TextView textOutput;
    private MaterialButton btnRunCode;

    public static Bundle createArgs(Problem problem) {
        Bundle args = new Bundle();
        if (problem == null) {
            return args;
        }

        args.putInt(ARG_PROBLEM_ID, problem.getId());
        args.putInt(ARG_PROBLEM_NUMBER, problem.getNumber());
        args.putString(ARG_PROBLEM_TITLE, problem.getTitle());
        args.putString(ARG_PROBLEM_SLUG, problem.getSlug());
        args.putString(ARG_PROBLEM_URL, problem.getUrl());
        args.putString(ARG_PROBLEM_DESCRIPTION, problem.getDescription());
        args.putString(ARG_PROBLEM_DIFFICULTY, problem.getDifficulty());
        args.putString(ARG_PROBLEM_TOPIC, problem.getTopic());
        args.putInt(ARG_PROBLEM_XP, problem.getXpReward());
        return args;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_problem_solver, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        compilerRepository = new CompilerRepository();
        currentProblem = readProblemArgs();

        spinnerLanguage = view.findViewById(R.id.spinner_language);
        inputCode = view.findViewById(R.id.input_code);
        inputStdin = view.findViewById(R.id.input_stdin);
        textStatus = view.findViewById(R.id.text_solver_status);
        textOutput = view.findViewById(R.id.text_solver_output);
        btnRunCode = view.findViewById(R.id.btn_run_code);

        bindProblem(view);
        setupLanguagePicker();
        setupActions(view);
    }

    private void bindProblem(View view) {
        TextView textNumber = view.findViewById(R.id.text_solver_problem_number);
        TextView textTitle = view.findViewById(R.id.text_solver_problem_title);
        TextView textMeta = view.findViewById(R.id.text_solver_problem_meta);
        TextView textDescription = view.findViewById(R.id.text_solver_problem_desc);

        if (currentProblem.getNumber() > 0) {
            textNumber.setText(getString(R.string.solver_problem_number, currentProblem.getNumber()));
        } else {
            textNumber.setText(currentProblem.getTopic());
        }

        textTitle.setText(safeText(currentProblem.getTitle(), getString(R.string.daily_challenge)));
        textMeta.setText(getString(
                R.string.solver_meta_format,
                safeText(currentProblem.getDifficulty(), getString(R.string.medium)),
                safeText(currentProblem.getTopic(), getString(R.string.topics)),
                currentProblem.getXpReward()
        ));

        String description = currentProblem.getDescription();
        textDescription.setText(hasText(description)
                ? description
                : getString(R.string.solver_description_fallback));

        textStatus.setText(R.string.solver_status_idle);
        textOutput.setText(R.string.solver_output_idle);
    }

    private void setupLanguagePicker() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                CodeLanguage.getDisplayNames()
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);
        spinnerLanguage.setSelection(0, false);
        spinnerLanguage.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                CodeLanguage nextLanguage = CodeLanguage.fromDisplayName(
                        String.valueOf(parent.getItemAtPosition(position))
                );
                String currentCode = readText(inputCode);
                String previousTemplate = lastTemplateApplied;
                selectedLanguage = nextLanguage;

                if (!hasText(currentCode) || (previousTemplate != null && previousTemplate.equals(currentCode))) {
                    applyTemplate(nextLanguage);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Keep the current language.
            }
        });
        applyTemplate(selectedLanguage);
    }

    private void setupActions(View view) {
        view.findViewById(R.id.btn_solver_back).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        view.findViewById(R.id.btn_open_problem).setOnClickListener(v -> openProblemLink());

        view.findViewById(R.id.btn_reset_code).setOnClickListener(v -> {
            applyTemplate(selectedLanguage);
            Toast.makeText(requireContext(), R.string.solver_reset_done, Toast.LENGTH_SHORT).show();
        });

        btnRunCode.setOnClickListener(v -> runCode());
    }

    private void runCode() {
        String sourceCode = readText(inputCode);
        if (!hasText(sourceCode)) {
            inputCode.setError(getString(R.string.solver_code_required));
            Toast.makeText(requireContext(), R.string.solver_code_required, Toast.LENGTH_SHORT).show();
            return;
        }

        inputCode.setError(null);
        setRunningState(true);
        textStatus.setText(R.string.solver_running);
        textOutput.setText(R.string.loading);

        compilerRepository.runCode(selectedLanguage, sourceCode, readText(inputStdin),
                new CompilerRepository.ExecutionCallback() {
                    @Override
                    public void onStatusChanged(String status) {
                        if (!isAdded() || getView() == null) {
                            return;
                        }
                        textStatus.setText(status);
                    }

                    @Override
                    public void onCompleted(CompilerSubmissionResultResponse result) {
                        if (!isAdded() || getView() == null) {
                            return;
                        }

                        setRunningState(false);
                        boolean success = compilerRepository.wasSuccessful(result);
                        textStatus.setText(success
                                ? getString(R.string.solver_success)
                                : resolveResultStatus(result));
                        textOutput.setText(compilerRepository.buildConsoleOutput(result));
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded() || getView() == null) {
                            return;
                        }

                        setRunningState(false);
                        textStatus.setText(message);
                        textOutput.setText(message);
                    }
                });
    }

    private void setRunningState(boolean running) {
        btnRunCode.setEnabled(!running);
        btnRunCode.setAlpha(running ? 0.7f : 1f);
    }

    private void openProblemLink() {
        String url = currentProblem.getResolvedUrl();
        if (!hasText(url)) {
            Toast.makeText(requireContext(), R.string.solver_no_problem_link, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(requireContext(), R.string.solver_no_problem_link, Toast.LENGTH_SHORT).show();
        }
    }

    private void applyTemplate(CodeLanguage language) {
        lastTemplateApplied = language.buildTemplate(currentProblem.getTitle());
        inputCode.setText(lastTemplateApplied);
        inputCode.setSelection(lastTemplateApplied.length());
    }

    private Problem readProblemArgs() {
        Problem fallback = Problem.getDailyChallenge();
        Bundle args = getArguments();
        if (args == null) {
            return fallback;
        }

        Problem problem = new Problem();
        problem.setId(args.getInt(ARG_PROBLEM_ID, fallback.getId()));
        problem.setNumber(args.getInt(ARG_PROBLEM_NUMBER, fallback.getNumber()));
        problem.setTitle(safeText(args.getString(ARG_PROBLEM_TITLE), fallback.getTitle()));
        problem.setSlug(safeText(args.getString(ARG_PROBLEM_SLUG), fallback.getSlug()));
        problem.setUrl(safeText(args.getString(ARG_PROBLEM_URL), fallback.getUrl()));
        problem.setDescription(safeText(args.getString(ARG_PROBLEM_DESCRIPTION), fallback.getDescription()));
        problem.setDifficulty(safeText(args.getString(ARG_PROBLEM_DIFFICULTY), fallback.getDifficulty()));
        problem.setTopic(safeText(args.getString(ARG_PROBLEM_TOPIC), fallback.getTopic()));
        problem.setXpReward(args.getInt(ARG_PROBLEM_XP, fallback.getXpReward()));
        return problem;
    }

    private String resolveResultStatus(CompilerSubmissionResultResponse result) {
        if (result != null && result.getStatus() != null && hasText(result.getStatus().getDescription())) {
            return result.getStatus().getDescription();
        }
        return getString(R.string.solver_failed);
    }

    private String readText(TextInputEditText input) {
        if (input == null || input.getText() == null) {
            return "";
        }
        return input.getText().toString().trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safeText(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }
}
