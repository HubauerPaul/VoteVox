# VoteVox – Voter Guide

*QR-code based digital voting system for HTL Wels*

---

## Table of Contents

1. [What is this about?](#1-what-is-this-about)
2. [Part A – How you vote in the test program](#part-a--how-you-vote-in-the-test-program)
   - A1. What you need
   - A2. Scan the QR code
   - A3. If a security warning appears
   - A4. Choose a candidate
   - A5. Confirm your choice
   - A6. Done
   - A7. If something does not work
3. [Part B – How it will be in the production version](#part-b--how-it-will-be-in-the-production-version)

---

## 1. What is this about?

With **VoteVox** you cast your vote in a school election (e.g. the school
representative election) **digitally with your phone**.

For this you receive a **printed card with a QR code**. This code is your
**personal, one-time voting key**:

- It allows **exactly one** vote.
- It is **anonymous** – your name is **not** stored. Nobody can tell whom you
  voted for.
- After voting the code is **used up** and no longer works.

> Treat the card like a ballot paper: do not pass it on, do not let anyone
> photograph it.

---

# Part A – How you vote in the test program

## A1. What you need

- Your **phone** with a camera.
- You are on the **same WiFi** as the voting PC (announced on site).
- Your **QR code card**.

## A2. Scan the QR code

1. Open the **camera app** (or a QR scanner) and point it at the QR code.
2. Tap the link that appears. The **“VoteVox”** voting page opens.

> **Alternative without a camera:** On the start page of the voting app there is,
> below “or”, an option to **enter the code by hand** (“Paste or type your code”).
> You find the code on your card.

## A3. If a security warning appears

In the test setup the browser may show a message like **“Your connection is not
secure”**. This is harmless inside the internal school network.

- Tap **“Advanced”** and then **“Proceed / Continue to the site”**.
- *(If the school has installed a certificate on the devices beforehand, this
  warning does not appear at all.)*

## A4. Choose a candidate

1. At the top you see the **title of the election**.
2. Below it the list of **candidates** (with name, class and department).
3. Tap the person you want to vote for – they become **highlighted**.
4. Tap **“Continue”**.

> You can select only **one** person.

## A5. Confirm your choice

1. On the confirmation page (**“Confirm your vote”**) you see once more **whom**
   you selected.
2. If everything is correct: tap **Confirm**.
   If you want to change something: tap **Back** and choose again.

> ⚠️ **Caution:** After confirming, your vote is **final** and **cannot** be
> changed.

## A6. Done

The confirmation **“Your vote has been recorded”** appears. You can now close the
page. **Thank you for taking part!**

## A7. If something does not work

| Message / problem | Meaning & what to do |
|-------------------|----------------------|
| Page does not open | Are you on the correct **WiFi**? Accept the security warning as in A3. If needed, enter the code by hand (A2). |
| “Missing voting code” | The link was incomplete. Scan the QR code again or enter the code by hand. |
| “Code already used” / rejected | This code has **already been used to vote**. Each code is valid only **once**. |
| Election is not active | The election has not started yet or has already ended. Tell the supervisor. |
| Camera does not open | Allow the QR scanner/camera app, or enter the code by hand. |

> If problems persist, contact the **election supervisor** on site.

---

# Part B – How it will be in the production version

In a later, real deployment at the school, voting will be **even simpler** for
you. The following is planned:

- **Vote from anywhere:** You will **no longer** need to be on the school WiFi. The
  voting page is reachable via a fixed internet address (e.g. `vote.htl-wels.at`) –
  even on mobile data.
- **No more security warning:** Thanks to an official certificate the page opens
  immediately and without any warning. The step from **A3 is gone completely**.
- **The camera works immediately** on all devices, without installing anything
  first.
- **Same simple flow:** scan → choose a candidate → confirm → done. Nothing about
  this changes.
- **Still fully anonymous:** the production version also does **not** store who
  voted how. Your QR code stays your one-time, anonymous key.

---

*Your vote counts – and stays secret.* 🗳️
