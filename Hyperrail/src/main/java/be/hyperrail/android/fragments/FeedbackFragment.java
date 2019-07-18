/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import be.hyperrail.android.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class FeedbackFragment extends Fragment {

    public static FeedbackFragment newInstance() {
        return new FeedbackFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_feedback, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final EditText vFeedbackText = view.findViewById(R.id.input_text);

        view.findViewById(R.id.button_send).setOnClickListener(view1 -> {
            Context context = FeedbackFragment.this.getActivity();
            // Default for version
            String version = "unknown";
            // Try to get the current version from the package
            try {
                PackageInfo pInfo = context.getPackageManager().getPackageInfo(
                        context.getPackageName(), 0);
                version = pInfo.versionName;
            } catch (Exception e) {
                // Ignored
            }

            Intent feedbackEmail = new Intent(Intent.ACTION_SEND);

            feedbackEmail.setType("text/email");
            feedbackEmail.putExtra(Intent.EXTRA_EMAIL, new String[]{"feedback@hyperrail.be"});
            feedbackEmail.putExtra(Intent.EXTRA_SUBJECT, "Feedback for hyperrail " + version);
            feedbackEmail.putExtra(Intent.EXTRA_TEXT, vFeedbackText.getText());
            startActivity(Intent.createChooser(feedbackEmail, "Send feedback mail:"));
        });
    }
}
