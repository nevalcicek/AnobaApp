package com.neval.anoba.di

import android.util.Log
import androidx.media3.common.util.UnstableApi
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.neval.anoba.chat.general.GeneralChatRepository
import com.neval.anoba.chat.general.GeneralChatViewModel
import com.neval.anoba.chat.group.GroupChatRepository
import com.neval.anoba.chat.group.GroupChatViewModel
import com.neval.anoba.chat.privatechat.PrivateChatRepository
import com.neval.anoba.chat.privatechat.PrivateChatViewModel
import com.neval.anoba.common.repository.IUserRepository
import com.neval.anoba.common.repository.UserRepository
import com.neval.anoba.common.services.AuthService
import com.neval.anoba.common.services.AuthServiceInterface
import com.neval.anoba.common.utils.EmailValidator
import com.neval.anoba.common.utils.EmailValidatorImpl
import com.neval.anoba.common.viewmodel.AuthViewModel
import com.neval.anoba.letter.LetterRepository
import com.neval.anoba.letter.LetterViewModel
import com.neval.anoba.letter.lettercomment.LetterCommentRepository
import com.neval.anoba.letter.lettercomment.LetterCommentViewModel
import com.neval.anoba.letter.letteremoji.LetterEmojiReactionRepository
import com.neval.anoba.livestream.LiveStreamViewModel
import com.neval.anoba.login.repository.ILoginRepository
import com.neval.anoba.login.repository.LoginRepository
import com.neval.anoba.login.viewmodel.ForgotPasswordViewModel
import com.neval.anoba.login.viewmodel.LoginViewModel
import com.neval.anoba.login.viewmodel.NewPasswordViewModel
import com.neval.anoba.login.viewmodel.SignUpViewModel
import com.neval.anoba.login.viewmodel.UserMigrationViewModel
import com.neval.anoba.photo.PhotoRepository
import com.neval.anoba.photo.PhotoViewModel
import com.neval.anoba.photo.photocomment.PhotoCommentRepository
import com.neval.anoba.photo.photocomment.PhotoCommentViewModel
import com.neval.anoba.photo.photoemoji.PhotoEmojiReactionRepository
import com.neval.anoba.ses.SesRepository
import com.neval.anoba.ses.SesViewModel
import com.neval.anoba.ses.sescomment.SesCommentRepository
import com.neval.anoba.ses.sescomment.SesCommentViewModel
import com.neval.anoba.ses.sesemoji.SesEmojiReactionRepository
import com.neval.anoba.video.CameraViewModel
import com.neval.anoba.video.VideoEditViewModel
import com.neval.anoba.video.VideoRepository
import com.neval.anoba.video.VideoViewModel
import com.neval.anoba.video.videocomment.VideoCommentRepository
import com.neval.anoba.video.videocomment.VideoCommentViewModel
import com.neval.anoba.video.videoemoji.VideoEmojiReactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

@androidx.annotation.OptIn(UnstableApi::class)
fun createAppModuleDefinition(
    authInstance: FirebaseAuth,
    firestoreInstance: FirebaseFirestore,
    storageInstance: FirebaseStorage
) = module {

    // Temel CoroutineScope
    single { CoroutineScope(SupervisorJob() + Dispatchers.IO) }

    // Firebase Bağımlılıkları
    single { authInstance }
    single { firestoreInstance }
    single { storageInstance }

    // --- SERVİS Tanımlamaları ---
    single<AuthServiceInterface> { AuthService(get(), get(), get(), get()) }
    single<EmailValidator> { EmailValidatorImpl() }

    // Coil ImageLoader (VideoFrameDecoder ile)
    single {
        ImageLoader.Builder(androidContext())
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }

    // --- REPOSITORY Tanımlamaları ---
    single<IUserRepository> { UserRepository(androidContext(), get(), get(), get()) }
    single<ILoginRepository> { LoginRepository(get()) }
    single { GeneralChatRepository(get()) }
    single { GroupChatRepository() }
    single { PrivateChatRepository() }

    // Letter Repositories
    single { LetterRepository(get()) }
    single { LetterCommentRepository(get()) }
    single { LetterEmojiReactionRepository(get()) }

    // Ses Repositories
    single { SesRepository(get(), get()) }
    single { SesEmojiReactionRepository(get()) }
    single { SesCommentRepository(get()) }

    // Photo Repositories
    single { PhotoRepository(get(), get()) }
    single { PhotoEmojiReactionRepository(get()) }
    single { PhotoCommentRepository(get()) }

    // Video Repositories
    single { VideoRepository(get(), get()) }
    single { VideoEmojiReactionRepository(get()) }
    single { VideoCommentRepository(get()) }

    // --- VIEWMODEL Tanımlamaları ---
    viewModel { AuthViewModel(get(), get()) }
    viewModel { LoginViewModel(get(), get(), get()) }
    viewModel { SignUpViewModel(get(), get()) }
    viewModel { ForgotPasswordViewModel(get(), get()) }
    viewModel { NewPasswordViewModel(get()) }
    viewModel { GeneralChatViewModel(get()) }
    viewModel { GroupChatViewModel(get(), get()) }
    viewModel { PrivateChatViewModel(get(), get(), get()) }

    // Letter ViewModels
    viewModel { LetterViewModel(get(), get(), get(), get()) }
    viewModel { (letterId: String) -> LetterCommentViewModel(letterId, get(), get()) }

    // Ses ViewModels
    viewModel { SesViewModel(get(), get(), get()) }
    viewModel { params -> SesCommentViewModel(params.get(), get(), get()) }

    // Photo ViewModels
    viewModel { PhotoViewModel(get(), get(), get(), get()) }
    viewModel { params -> PhotoCommentViewModel(params.get(), get(), get()) }

    // Video ViewModels
    viewModel { VideoViewModel(get(), get(), get()) }
    viewModel { params -> VideoCommentViewModel(params.get(), get(), get()) }
    viewModel { VideoEditViewModel() }
    viewModel { CameraViewModel() }

    // Diğer ViewModels
    viewModel { LiveStreamViewModel() }
    viewModel { UserMigrationViewModel(get()) }

}.apply {
    Log.d("AppModule", "Dependency injection module (createAppModuleDefinition) loaded successfully!")
}
