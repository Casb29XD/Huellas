package com.desarrolloMovielexample.huella.domain.usecase

import com.desarrolloMovielexample.huella.domain.model.Comment
import com.desarrolloMovielexample.huella.domain.model.Publication
import com.desarrolloMovielexample.huella.domain.model.PublicationDraft
import com.desarrolloMovielexample.huella.domain.repository.HuellaRepository
import kotlinx.coroutines.flow.StateFlow

class GetPublicationsUseCase(private val repository: HuellaRepository) {
    operator fun invoke(): StateFlow<List<Publication>> = repository.publications
}

class GetMyPublicationsUseCase(private val repository: HuellaRepository) {
    operator fun invoke(): StateFlow<List<Publication>> = repository.myPublications
}

class GetPublicationDetailUseCase(private val repository: HuellaRepository) {
    operator fun invoke(id: String): Publication? = repository.getPublication(id)
}

class CreatePublicationUseCase(private val repository: HuellaRepository) {
    suspend operator fun invoke(draft: PublicationDraft): Result<Publication> {
        if (draft.title.isBlank()) return Result.failure(IllegalArgumentException("El título es obligatorio"))
        if (draft.description.isBlank()) return Result.failure(IllegalArgumentException("La descripción es obligatoria"))
        return repository.createPublication(draft)
    }
}

class UpdatePublicationUseCase(private val repository: HuellaRepository) {
    suspend operator fun invoke(id: String, draft: PublicationDraft): Result<Publication> {
        if (draft.title.isBlank()) return Result.failure(IllegalArgumentException("El título es obligatorio"))
        return repository.updatePublication(id, draft)
    }
}

class DeletePublicationUseCase(private val repository: HuellaRepository) {
    suspend operator fun invoke(id: String): Result<Unit> = repository.deletePublication(id)
}

class VotePublicationUseCase(private val repository: HuellaRepository) {
    suspend operator fun invoke(id: String, value: Int): Result<Unit> = repository.votePublication(id, value)
}

class GetCommentsUseCase(private val repository: HuellaRepository) {
    operator fun invoke(publicationId: String): StateFlow<List<Comment>> = repository.comments(publicationId)
}

class AddCommentUseCase(private val repository: HuellaRepository) {
    suspend operator fun invoke(publicationId: String, text: String): Result<Comment> {
        if (text.isBlank()) return Result.failure(IllegalArgumentException("El comentario no puede estar vacío"))
        if (text.length > 500) return Result.failure(IllegalArgumentException("Máximo 500 caracteres"))
        return repository.addComment(publicationId, text)
    }
}

class DeleteCommentUseCase(private val repository: HuellaRepository) {
    suspend operator fun invoke(publicationId: String, commentId: String): Result<Unit> =
        repository.deleteComment(publicationId, commentId)
}

class ModeratePublicationUseCase(private val repository: HuellaRepository) {
    suspend fun approve(itemId: String): Result<Unit> = repository.approve(itemId)
    suspend fun reject(itemId: String, reason: String, comment: String): Result<Unit> =
        repository.reject(itemId, reason, comment)
}

class DeleteAccountUseCase(private val repository: HuellaRepository) {
    suspend operator fun invoke(): Result<Unit> = repository.deleteAccount()
}
