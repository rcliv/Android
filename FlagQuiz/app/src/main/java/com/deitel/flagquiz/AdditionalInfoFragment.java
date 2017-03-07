package com.deitel.flagquiz;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by robbylagen on 3/6/17.
 */

public class AdditionalInfoFragment extends Fragment {

    final static String ADD_INFO_REGION = "addInfoText";
    final static String ADD_INFO_FLAG = "addInfoFlag";
    final static String ADD_INFO_QNUM = "addInfoQNum";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view =
                inflater.inflate(R.layout.additional_info_frag, container, false);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        Bundle args = getArguments();
        if (args != null)
        {
            updateTextView(args.getString(ADD_INFO_FLAG), args.getString(ADD_INFO_REGION), args.getInt(ADD_INFO_QNUM));
        }

    }

    public void updateTextView(String addInfoFlag, String addInfoRegion, int qNum) {
        TextView questionNum = (TextView) getActivity().findViewById(R.id.addInfoQNumberTextView);
        questionNum.setText(getString(R.string.question, (qNum), 10));

        TextView article = (TextView) getActivity().findViewById(R.id.addInfoTextView);
        String addInfoText = "Region: " + addInfoRegion + "\nFlag: " + addInfoFlag + "\nName: Robby Lagen";
        article.setText(addInfoText);

        TextView correctAns = (TextView) getActivity().findViewById(R.id.addInfoAnswerTextView);
        String cAns = addInfoFlag + "!";
        correctAns.setText(cAns);
    }

}
