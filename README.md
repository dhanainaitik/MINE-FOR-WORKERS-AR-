MineSafe AR ⛏️

AR-Based Coal Mine Safety Training Simulator

MineSafe AR is an Android Augmented Reality (AR) training simulator
designed to help mine workers practice emergency-response procedures in
a safe, controlled, and measurable virtual underground mine.

Problem

Mining emergencies such as electrical fires and chemical hazards require
workers to respond quickly and correctly. Traditional training can
explain procedures theoretically, but it does not always allow workers
to repeatedly experience an emergency and perform the required actions
without entering a hazardous environment.

Solution

MineSafe AR places an optimized 3D underground mine into the worker’s
physical environment using AR.

Core experience

Real Room → AR Floor Scanning → Black AR Doorway → Mine Entrance →
Underground Tunnel → Mine Environment → Safety Scenario → Worker Action
→ Score & Performance

The worker physically approaches and enters the AR doorway, then
experiences a connected underground mine environment.

Training Modules

Electrical Fire Safety

Typical flow:

Electrical fire detected

Voice warning

Worker moves toward the fire extinguisher

Worker reaches the target area

Extinguisher interaction

Aim toward the fire

Discharge

Fire extinguished

Performance evaluation

Tracked metrics can include response time, completed steps, mistakes,
and safety score.

Chemical Hazard Safety

Typical flow:

Chemical hazard detected

Identify the hazard

Maintain safe distance

Follow the required safety procedure

Isolate/report the hazard

Complete training

Performance evaluation

AR Mine Environment

The intended environment flow is:

REAL WORLD
    ↓
BLACK AR DOORWAY
    ↓
MINE ENTRANCE
    ↓
POSITANOS TUNNEL
    ↓
OPEN TUNNEL END
    ↓
CARRIÈRE ORLÉANS MINE
    ↓
TRAINING SCENARIO

The tunnel and mine should appear as one continuous environment with no
unintended gap, blocking wall, artificial exit, or real-world background
visible through the mine.

Movement Amplification

The prototype is designed for use in a relatively small physical room.

Approximately:

1 metre physical movement
        ≈
10 metres virtual mine movement

This allows the worker to experience a much longer virtual mine without
physically walking the entire mine.

Technology Stack

Android

Kotlin

ARCore / Android AR capabilities

GLB / glTF 3D assets

Antigravity IDE for development and integration

Meshy for 3D asset generation

Blender for optional polygon reduction and asset optimization

Gradle for Android builds

3D Asset Pipeline

Meshy / 3D Assets
        ↓
Blender Optimization (when required)
        ↓
Optimized GLB
        ↓
Antigravity
        ↓
Android APK

The project prioritizes mobile-friendly geometry, optimized textures,
reusable materials, and lightweight collision geometry.

AI

AI can support multilingual voice guidance, training content,
explanations, and future personalized feedback.

Safety-critical scenario progression, distance thresholds, required
actions, and scoring should remain deterministic application logic
rather than depending on an AI response.

Performance Tracking

Potential training metrics:

Metric

Description

Safety Score

Overall training performance

Response Time

Time taken to respond

Mistakes

Incorrect actions

Steps Completed

Required actions completed

Module

Training scenario

Completion Status

Training result

A future backend can send these results to an administrator dashboard.

Future Architecture

Android AR App
      ↓
Backend API
      ↓
Database
      ↓
Admin Dashboard

This can support worker profiles, training history, analytics, and
instructor monitoring.

Mobile Optimization

Because MineSafe AR targets Android devices:

Reduce unnecessary polygon counts

Optimize GLB assets

Compress textures appropriately

Use simple collision geometry

Reuse materials

Avoid unnecessary scene objects

Use realistic lighting without excessive rendering cost

Prefer optimized modular assets over one extremely large model

Project Structure

mera-mine/
├── app/
│   └── src/main/
│       ├── java/
│       ├── res/
│       └── AndroidManifest.xml
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── mine_entrance.glb
├── positanos_tunnel_optimized.glb
├── carriere_orleans_1_optimized.glb
└── README.md

Safety Disclaimer

MineSafe AR is a training simulator. It is not a replacement for
official mine safety procedures, certifications, supervision, or
site-specific emergency protocols. Training content should be validated
against the procedures of the relevant organization and domain experts.

Vision

MineSafe AR aims to transform mine safety training from a primarily
theoretical experience into a repeatable, immersive, and measurable
simulation.

Enter. Experience. Respond. Learn.

Project Status

Current stage: Android AR prototype

Current development focuses on:

AR floor detection

AR doorway placement

Underground mine environment

Connected tunnel and mine experience

Emergency training flows

Voice guidance

Performance scoring

Android APK deployment

Real-device AR testing

Developed as a Smart India Hackathon project.
