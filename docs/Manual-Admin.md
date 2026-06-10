# VoteVox – Administrator Manual

*QR-code based digital voting system for HTL Wels*

---

## Table of Contents

1. [About this manual](#1-about-this-manual)
2. [System overview](#2-system-overview)
3. [Part A – Test program (current version)](#part-a--test-program-current-version)
   - A1. Start the system
   - A2. Sign in
   - A3. Manage classes
   - A4. Create an election
   - A5. Add candidates
   - A6. Select participating classes
   - A7. Generate and print voting codes (QR codes)
   - A8. Start, monitor and stop the election
   - A9. View results
   - A10. Audit log
   - A11. Stop the system
   - A12. Sending the project to someone else
   - A13. Troubleshooting
4. [Part B – Production version (outlook)](#part-b--production-version-outlook)

---

## 1. About this manual

This manual is for **administrators** (e.g. teachers or the election committee)
who prepare and run an election with VoteVox.

It has two parts:

- **Part A – Test program:** describes the system exactly as it runs today
  (diploma project / demo) on a single PC.
- **Part B – Production version:** describes what the system would look like in a
  later, productive deployment at the school.

> **Terms**
> - **Admin UI:** the management interface (administrators only).
> - **Voting UI:** the voting interface students use on their phones.
> - **Voting code / token:** a one-time, anonymous code printed as a QR code that
>   allows exactly one vote to be cast.

---

## 2. System overview

VoteVox consists of four building blocks that work together automatically on
start-up:

| Building block | Purpose                                            | Address (test program)        |
|----------------|----------------------------------------------------|-------------------------------|
| Database       | Stores elections, classes, codes and votes         | (internal)                    |
| Backend        | The “logic”: validates codes, counts votes         | (internal, port 8080)         |
| Admin UI       | Management of elections                            | `https://localhost:5174`      |
| Voting UI      | Vote casting by students                           | `https://<PC-IP>:5173/vote`   |

**Core principle – anonymity:** **No student names** are stored. Per participating
class, only a number of anonymous voting codes is generated. The system does not
know which code belongs to which person – only the class is recorded. Each code can
be used **exactly once**.

---

# Part A – Test program (current version)

## A1. Start the system

1. Make sure **Docker Desktop** is installed.
2. Double-click **`VoteVox starten.bat`** in the project folder.
3. A window opens that automatically:
   - checks whether Docker is running (and starts Docker if needed),
   - detects the PC's current network address (IP),
   - creates an HTTPS certificate for that address,
   - builds and starts all building blocks,
   - opens the admin interface in the browser.

> **On the very first start**, Docker downloads many components and builds the
> system – this can take a few minutes. After that it is much faster.

At the end the window shows a summary with all important addresses, including the
voting address for the phones (e.g. `https://192.168.178.44:5173/vote`). This
window can be closed – VoteVox keeps running in the background.

> ⚠️ **Important:** If Windows asks for a **firewall exception** on the first
> start, it must be **allowed**, otherwise phones cannot reach the PC.

## A2. Sign in

1. The admin interface opens at `https://localhost:5174`.
2. Default credentials in the test program:
   - **E-mail:** `admin@votevox.at`
   - **Password:** `Admin1234!`
3. After signing in you land on the **Dashboard** with an overview.

> If the browser shows a certificate warning: there should be none on the PC. For
> phones see [A7](#a7-generate-and-print-voting-codes-qr-codes).

## A3. Manage classes

Before an election can be run, the participating **classes** must exist.

1. Open the **“Classes”** menu item.
2. Use **“Add class”** to create a class:
   - **Name** of the class (e.g. `4AHIT`),
   - **Student count**.
3. The count determines how many anonymous voting codes are generated for that
   class.

> The test program already contains three sample classes (e.g. `4AHIT`, `4BHIT`,
> `4AHWI`).

## A4. Create an election

1. **“Elections”** → **“New election”**.
2. Fill in the fields:
   - **Title** (e.g. “School Representative Election 2026”),
   - **Type** of election,
   - **Description** (optional),
   - **Start date** and **End date**.
3. Save. The election now has the status **“Planned”**.

> An election moves through the states **Planned → Running → (Stopped/Closed)**.
> Voting codes can only be generated while the status is **Planned**.

## A5. Add candidates

1. Open the election and switch to the **“Candidates”** tab.
2. For each candidate enter:
   - **Name**,
   - **Class**,
   - **Department**.
3. You can add any number of candidates (while the election is **Planned**).

## A6. Select participating classes

1. In the election switch to the **“Classes”** tab.
2. Tick the classes that may take part in this election (use **“Select all”** to
   select all at once).
3. The sum of the student counts of all ticked classes is the number of **eligible
   votes** – and therefore the number of codes to be generated.

## A7. Generate and print voting codes (QR codes)

1. In the election switch to the **“Tokens”** tab.
2. Click **“Generate Tokens”** and confirm.
3. The system creates as many anonymous codes per participating class as there are
   students.
4. The codes are shown in plain text **only once**.
5. **Immediately** click **“Generate PDF with QR Codes”** and save/print the PDF.
   The PDF contains 12 QR codes per A4 page (3×4) with cut lines, **grouped by
   class** (one class per page).
6. Cut the printed sheet along the cut lines – each card is one QR code for one
   vote.

> 🔒 **Important:** The plain-text codes are stored only **hashed (encrypted)** in
> the database and **cannot be recovered** afterwards. If you do not save the PDF,
> you must reset and regenerate the codes.
>
> **Lost the PDF?** As long as the election is still *Planned*, the Tokens tab
> offers **“Reset & re-generate codes”**. This invalidates the old codes and
> creates a completely new set. Previously printed codes will then stop working.

### QR codes on phones without a warning (once per device)

So that scanning shows **no** “connection not secure” warning and the **camera**
works, the root certificate should be installed once:

1. Copy **`votevox-rootCA.pem`** from the project folder to the phone (e.g. by
   e-mail or USB).
2. Install it on the phone (Android: *Settings → Security → Install a certificate*;
   iOS: install the profile **and** enable it under *Settings → General → About →
   Certificate Trust Settings*).

> Even without this installation voting usually works – the phone then shows a
> warning that must be accepted.
>
> *(If the launcher had to fall back to a self-signed certificate, there is no
> `votevox-rootCA.pem` to install – simply accept the one-time warning.)*

## A8. Start, monitor and stop the election

- **Start:** in the election, top right, **“Start Election”**. From now on votes
  can be cast. Codes can **no longer** be generated or reset.
- **Monitor:** the **“Overview”** tab continuously shows the number of **votes
  cast** versus eligible voters.
- **Stop:** **“Stop Election”**. After this no further votes are possible.

> A **running** election cannot be deleted. Stop it first, then delete if needed.

## A9. View results

1. In the election switch to the **“Results”** tab.
2. There you can see:
   - **Votes per candidate** (with a chart),
   - **Turnout per class** (how many codes were issued/used).

> Because no names are stored, it is not possible to tell **who** voted how – only
> **how many** votes each candidate received.

## A10. Audit log

The **“Audit”** menu item shows a log of the important actions (e.g. election
started/stopped, codes generated). This serves the traceability of the election
process – without any link to individual voters.

## A11. Stop the system

- Double-click **`VoteVox stoppen.bat`**. This stops all building blocks.
- The **data is preserved** – on the next start everything is back.
- To start over with an **empty** database, run the stop script with the `-Wipe`
  option (deletes **all** data irreversibly).

## A12. Sending the project to someone else

The recipient builds everything from source with Docker, so you only send the
source – **not** the generated folders.

1. Double-click **`Projekt-packen.bat`**. It creates a clean
   `VoteVox-<date>.zip` (without `node_modules`, build output, certificates,
   `.git`) in the folder above the project.
2. Send that single `.zip`.
3. The recipient:
   - installs **Docker Desktop** (and starts it once),
   - unzips the file,
   - double-clicks **`VoteVox starten.bat`**.

Everything else (network address, certificate, build) happens automatically on
the recipient's machine.

## A13. Troubleshooting

| Problem | Cause / fix |
|---------|-------------|
| “Docker is not running” | Start Docker Desktop and wait until it says “Running”; start the script again. |
| Phone does not open the voting page | The phone must be on the **same WiFi** as the PC. Allow the Windows firewall exception for ports 5173/5174. |
| Certificate warning on the phone | Install `votevox-rootCA.pem` on the phone (see A7), or accept the one-time warning. |
| QR codes point to the wrong/old address | Did the PC's IP change? Just run **`VoteVox stoppen.bat`** then **`VoteVox starten.bat`** – the new IP is written into the QR codes automatically. **Then regenerate and reprint the codes.** |
| “Generate Tokens” is greyed out | The election is not *Planned*, or no class is selected. |
| Code rejected when voting | The code was already used, belongs to a different election, or the election is not running (any more). |

---

# Part B – Production version (outlook)

This part describes what VoteVox would look like in a **real, permanent
deployment** at the school. These points are **not yet** part of the test program
but the planned next step.

## B1. Central server installation instead of a single PC

Instead of running on a teacher's laptop, VoteVox runs on a **central school
server** (or a hosted server). Advantages:

- Reachable via a fixed **internet address / domain**
  (e.g. `https://vote.htl-wels.at`) instead of a changing PC IP.
- Voters no longer need to be on the same WiFi.
- No firewall configuration on an individual PC required.

## B2. A real, trusted certificate

The production version uses an **official TLS certificate** (e.g. via Let's
Encrypt) for the school domain. This removes:

- the manual installation of `votevox-rootCA.pem` on every phone,
- every certificate warning.

The camera works on all devices immediately.

## B3. Multiple administrator accounts & password policy

- Personal admin accounts instead of one shared default login.
- Forced password change on first login, strong password policy.
- Optional: roles (e.g. “election manager” vs. “read/results only”).
- Integration with the existing school sign-in (e.g. Microsoft 365 / LDAP).

## B4. Security & operations

- Strong, randomly generated **JWT secret** and **DB password** (not the test
  defaults).
- Regular automatic **database backups**.
- Encrypted connections throughout, a hardened server operating system.
- Logging/monitoring for operations.

## B5. Convenience features (possible extensions)

- **Per-class code distribution** as separate PDFs / batch printing.
- A live turnout dashboard on a projector (without interim results).
- Export of the results as a PDF report for the election committee.
- Multilingual interface (DE/EN).
- A status view of which classes have not yet fully voted (still anonymous, only
  at class level).

---

*Status: test program of the diploma project. The default credentials and test
secrets are intended for the demo only and must be changed before any real use.*
