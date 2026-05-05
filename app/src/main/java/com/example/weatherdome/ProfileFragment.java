package com.example.weatherdome;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment{
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile,container,false);
        EditText etusername = view.findViewById(R.id.et_username);
        EditText etbio = view.findViewById(R.id.et_bio);
        ImageView ivavatar = view.findViewById(R.id.iv_avatar);
        SharedPreferences sp =requireActivity().getSharedPreferences("UserData", Context.MODE_PRIVATE);
        String savedName = sp.getString("NAME","");
        etusername.setText(savedName);

        String savedBio = sp.getString("BIO","");
        etbio.setText(savedBio);

        String savedAvatar = sp.getString("AVATAR_GENDER", "unknown");
        if(savedAvatar.equals("boy")){
            ivavatar.setImageResource(R.drawable.boy);

        }else if(savedAvatar.equals("girl")){
            ivavatar.setImageResource(R.drawable.girl);
        }



    //焦点变化
    etusername.setOnFocusChangeListener(new View.OnFocusChangeListener(){
        @Override
        public  void onFocusChange(View v,boolean hasFocus){
            if(!hasFocus){
                //即hasFocus为false已输入完成光标离开
                String newName = etusername.getText().toString();
                sp.edit().putString("NAME",newName).apply();
                Toast.makeText(requireContext(),"昵称已保存",Toast.LENGTH_LONG).show();

            }
        }
    });
    etbio.setOnFocusChangeListener(new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if(!hasFocus){
                String newBio = etbio.getText().toString();
                sp.edit().putString("BIO",newBio).apply();
                Toast.makeText(requireContext(),"个性签名已保存",Toast.LENGTH_LONG).show();

            }
        }
    });

        ivavatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String[] options = {"👦 切换为男生头像", "👧 切换为女生头像"};

                new AlertDialog.Builder(requireContext())
                        .setTitle("请选择你的默认头像")
                        .setItems(options, (dialog, which) -> {
                            if (which == 0) {
                                ivavatar.setImageResource(R.drawable.boy);
                                sp.edit().putString("AVATAR_GENDER", "boy").apply();
                                Toast.makeText(requireContext(), "已换上男生头像！", Toast.LENGTH_SHORT).show();

                            } else if (which == 1) {
                                ivavatar.setImageResource(R.drawable.girl);
                                sp.edit().putString("AVATAR_GENDER", "girl").apply();
                                Toast.makeText(requireContext(), "已换上女生头像！", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .show();
            }
        });
        return view;

    }

}
