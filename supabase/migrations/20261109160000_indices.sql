-- Ítem 19: índices en las FK y en las columnas que filtran/ordenan la app y las políticas RLS.
-- Las FK ya cubiertas por una PK/UNIQUE con esa columna al inicio no se repiten
-- (adoption_requests.publication_id, reports.publication_id, publication_votes.publication_id).

CREATE INDEX IF NOT EXISTS publications_author_id_idx ON public.publications (author_id);
CREATE INDEX IF NOT EXISTS publications_status_created_at_idx ON public.publications (status, created_at DESC);
CREATE INDEX IF NOT EXISTS publications_category_created_at_idx ON public.publications (category, created_at DESC);
CREATE INDEX IF NOT EXISTS publications_city_idx ON public.publications (city);
CREATE INDEX IF NOT EXISTS publications_lifecycle_active_idx ON public.publications (category, created_at)
    WHERE lifecycle = 'ACTIVA';

CREATE INDEX IF NOT EXISTS adoption_requests_applicant_id_idx ON public.adoption_requests (applicant_id);

CREATE INDEX IF NOT EXISTS conversations_participant_one_idx ON public.conversations (participant_one);
CREATE INDEX IF NOT EXISTS conversations_participant_two_idx ON public.conversations (participant_two);
CREATE INDEX IF NOT EXISTS conversations_publication_id_idx ON public.conversations (publication_id);

CREATE INDEX IF NOT EXISTS chat_messages_conversation_created_idx ON public.chat_messages (conversation_id, created_at);
CREATE INDEX IF NOT EXISTS chat_messages_sender_id_idx ON public.chat_messages (sender_id);

CREATE INDEX IF NOT EXISTS reports_reporter_id_idx ON public.reports (reporter_id);
CREATE INDEX IF NOT EXISTS reports_resolved_by_idx ON public.reports (resolved_by);
CREATE INDEX IF NOT EXISTS reports_open_idx ON public.reports (created_at DESC) WHERE status = 'ABIERTO';

CREATE INDEX IF NOT EXISTS notifications_user_created_idx ON public.notifications (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS notifications_read_created_idx ON public.notifications (created_at) WHERE read;

CREATE INDEX IF NOT EXISTS points_history_user_created_idx ON public.points_history (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS comments_publication_created_idx ON public.comments (publication_id, created_at);
CREATE INDEX IF NOT EXISTS comments_author_id_idx ON public.comments (author_id);
CREATE INDEX IF NOT EXISTS publication_votes_user_id_idx ON public.publication_votes (user_id);
CREATE INDEX IF NOT EXISTS sightings_publication_id_idx ON public.sightings (publication_id);
CREATE INDEX IF NOT EXISTS sightings_reporter_id_idx ON public.sightings (reporter_id);
CREATE INDEX IF NOT EXISTS publication_ai_reviews_publication_idx ON public.publication_ai_reviews (publication_id, created_at DESC);
