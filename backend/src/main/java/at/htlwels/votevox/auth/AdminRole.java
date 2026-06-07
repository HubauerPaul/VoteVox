package at.htlwels.votevox.auth;

/**
 * Authorization role for admin users of the VoteVox backoffice.
 */
public enum AdminRole {
    /** Limited rights: view results and audit log. */
    TEACHER,
    /** Full administrative rights: create/manage elections, students, tokens, backups. */
    ADMIN
}
