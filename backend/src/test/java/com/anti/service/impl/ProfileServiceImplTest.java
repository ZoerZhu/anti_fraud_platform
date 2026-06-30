package com.anti.service.impl;

import com.anti.common.BusinessException;
import com.anti.entity.UserProfile;
import com.anti.mapper.CaseBrowseLogMapper;
import com.anti.mapper.NewsBrowseLogMapper;
import com.anti.mapper.UserMapper;
import com.anti.mapper.UserProfileMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock
    private UserProfileMapper profileMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CaseBrowseLogMapper caseBrowseLogMapper;

    @Mock
    private NewsBrowseLogMapper newsBrowseLogMapper;

    @InjectMocks
    private ProfileServiceImpl service;

    @Test
    void addWeakPointTrimsAndSkipsDuplicateTag() {
        UserProfile profile = profile();
        profile.setWeakPoints("[\"钓鱼网站\"]");
        when(profileMapper.selectByUserId(1L)).thenReturn(profile);

        service.addWeakPoint(1L, "  钓鱼网站  ");

        verify(profileMapper, never()).updateById(profile);
    }

    @Test
    void addInterestTagRejectsBlankTag() {
        when(profileMapper.selectByUserId(1L)).thenReturn(profile());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.addInterestTag(1L, "   "));

        assertThat(exception.getCode()).isEqualTo(400);
        assertThat(exception.getMessage()).contains("兴趣标签不能为空");
    }

    private UserProfile profile() {
        UserProfile profile = new UserProfile();
        profile.setId(1L);
        profile.setUserId(1L);
        profile.setBrowseCount(0);
        profile.setRegisterDays(0);
        profile.setLifecycleStage("newbie");
        return profile;
    }
}
