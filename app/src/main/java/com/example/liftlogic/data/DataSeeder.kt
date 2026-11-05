package com.example.liftlogic.data

import androidx.room.withTransaction
import com.example.liftlogic.data.db.AppDatabase
import com.example.liftlogic.data.db.ExerciseEntity
import com.example.liftlogic.data.db.RoutineEntity
import com.example.liftlogic.data.db.RoutineExerciseEntity

/** Fills the database with the built-in exercise library and training programs on first launch. */
class DataSeeder(private val db: AppDatabase) {

    private class Ex(val name: String, val muscle: MuscleGroup, val equipment: Equipment, val how: String)

    private class Item(val exercise: String, val sets: Int, val reps: String, val rest: Int)

    private class Program(val name: String, val description: String, val items: List<Item>)

    suspend fun seedIfNeeded() {
        val exerciseDao = db.exerciseDao()
        val routineDao = db.routineDao()
        if (exerciseDao.countBuiltIn() > 0 && routineDao.countBuiltIn() > 0) return

        db.withTransaction {
            if (exerciseDao.countBuiltIn() == 0) {
                exerciseDao.insertAll(exercises.map {
                    ExerciseEntity(name = it.name, muscleGroup = it.muscle.key, equipment = it.equipment.key, instructions = it.how)
                })
            }
            if (routineDao.countBuiltIn() == 0) {
                val idsByName = exerciseDao.getBuiltIn().associate { it.name to it.exerciseId }
                for (program in programs) {
                    val routineId = routineDao.insertRoutine(
                        RoutineEntity(ownerUserId = null, name = program.name, description = program.description)
                    )
                    routineDao.insertItems(program.items.mapIndexedNotNull { index, item ->
                        val exerciseId = idsByName[item.exercise] ?: return@mapIndexedNotNull null
                        RoutineExerciseEntity(
                            routineId = routineId,
                            exerciseId = exerciseId,
                            position = index,
                            targetSets = item.sets,
                            targetReps = item.reps,
                            restSec = item.rest
                        )
                    })
                }
            }
        }
    }

    private val exercises = listOf(
        // Chest
        Ex("Bench Press", MuscleGroup.CHEST, Equipment.BARBELL, "Lie on a flat bench, grip the bar slightly wider than shoulders. Lower it to mid-chest with control, then press up until your arms are straight. Keep your feet planted and shoulder blades pinched."),
        Ex("Incline Bench Press", MuscleGroup.CHEST, Equipment.BARBELL, "Set the bench to 30–45°. Lower the bar to your upper chest and press it back up over your shoulders."),
        Ex("Dumbbell Bench Press", MuscleGroup.CHEST, Equipment.DUMBBELL, "Lie on a flat bench with a dumbbell in each hand. Lower them to the sides of your chest, then press up and slightly together."),
        Ex("Incline Dumbbell Press", MuscleGroup.CHEST, Equipment.DUMBBELL, "On a 30–45° incline, press the dumbbells from upper-chest level to above your shoulders."),
        Ex("Dumbbell Fly", MuscleGroup.CHEST, Equipment.DUMBBELL, "With a slight bend in the elbows, open your arms wide until you feel a stretch in the chest, then bring the dumbbells back together over your chest."),
        Ex("Cable Crossover", MuscleGroup.CHEST, Equipment.CABLE, "Stand between two high pulleys, step forward and bring the handles together in front of your chest in a hugging motion."),
        Ex("Chest Press Machine", MuscleGroup.CHEST, Equipment.MACHINE, "Adjust the seat so the handles are at chest height. Press forward until your arms are extended, then return slowly."),
        Ex("Push-Up", MuscleGroup.CHEST, Equipment.BODYWEIGHT, "Hands just wider than shoulders, body in a straight line. Lower your chest to the floor and push back up."),
        Ex("Dips", MuscleGroup.CHEST, Equipment.BODYWEIGHT, "Support yourself on parallel bars, lean slightly forward and lower until your shoulders are just below your elbows, then press up."),

        // Back
        Ex("Deadlift", MuscleGroup.BACK, Equipment.BARBELL, "Stand with mid-foot under the bar, grip just outside your legs. Brace, keep a flat back and stand up by driving through the floor. Lower the bar along your legs."),
        Ex("Barbell Row", MuscleGroup.BACK, Equipment.BARBELL, "Hinge forward with a flat back. Pull the bar to your lower ribs, squeezing your shoulder blades, then lower with control."),
        Ex("Pull-Up", MuscleGroup.BACK, Equipment.BODYWEIGHT, "Hang from a bar with an overhand grip. Pull your chest towards the bar, then lower all the way down."),
        Ex("Chin-Up", MuscleGroup.BACK, Equipment.BODYWEIGHT, "Like a pull-up but with palms facing you. Pull until your chin is over the bar."),
        Ex("Lat Pulldown", MuscleGroup.BACK, Equipment.CABLE, "Sit with thighs under the pads, pull the bar down to your upper chest while leaning back slightly, then let it rise slowly."),
        Ex("Seated Cable Row", MuscleGroup.BACK, Equipment.CABLE, "Sit tall, pull the handle to your stomach while squeezing the shoulder blades together, then extend your arms."),
        Ex("Dumbbell Row", MuscleGroup.BACK, Equipment.DUMBBELL, "Support one knee and hand on a bench. Row the dumbbell to your hip, keeping your back flat."),
        Ex("T-Bar Row", MuscleGroup.BACK, Equipment.BARBELL, "Straddle the bar, hinge forward and row the handle to your chest."),
        Ex("Face Pull", MuscleGroup.BACK, Equipment.CABLE, "Using a rope at face height, pull towards your forehead with elbows high, rotating your hands outward."),
        Ex("Back Extension", MuscleGroup.BACK, Equipment.BODYWEIGHT, "On a hyperextension bench, lower your torso with a flat back and raise it until your body is straight."),

        // Legs
        Ex("Back Squat", MuscleGroup.LEGS, Equipment.BARBELL, "Bar on your upper back, feet shoulder-width. Sit down and back until thighs are at least parallel, keep your chest up, then drive up."),
        Ex("Front Squat", MuscleGroup.LEGS, Equipment.BARBELL, "Hold the bar on the front of your shoulders with elbows high. Squat down keeping your torso upright."),
        Ex("Romanian Deadlift", MuscleGroup.LEGS, Equipment.BARBELL, "With soft knees, push your hips back and lower the bar along your legs until you feel a hamstring stretch, then stand up."),
        Ex("Leg Press", MuscleGroup.LEGS, Equipment.MACHINE, "Feet shoulder-width on the platform. Lower until knees are around 90°, then press without locking your knees."),
        Ex("Walking Lunge", MuscleGroup.LEGS, Equipment.DUMBBELL, "Step forward and lower until both knees are about 90°, then step through into the next lunge."),
        Ex("Bulgarian Split Squat", MuscleGroup.LEGS, Equipment.DUMBBELL, "Rear foot on a bench, lower straight down on the front leg, then push back up."),
        Ex("Leg Extension", MuscleGroup.LEGS, Equipment.MACHINE, "Extend your knees until your legs are straight, squeeze the quads, then lower slowly."),
        Ex("Lying Leg Curl", MuscleGroup.LEGS, Equipment.MACHINE, "Lie face down and curl the pad towards your glutes, then lower with control."),
        Ex("Hip Thrust", MuscleGroup.LEGS, Equipment.BARBELL, "Upper back on a bench, bar over your hips. Drive your hips up until your body is straight from knees to shoulders."),
        Ex("Goblet Squat", MuscleGroup.LEGS, Equipment.KETTLEBELL, "Hold a kettlebell or dumbbell at your chest and squat down between your knees."),
        Ex("Standing Calf Raise", MuscleGroup.LEGS, Equipment.MACHINE, "Rise onto your toes as high as possible, pause, then lower your heels below the platform."),

        // Shoulders
        Ex("Overhead Press", MuscleGroup.SHOULDERS, Equipment.BARBELL, "Standing, press the bar from your shoulders to overhead, moving your head back slightly as it passes. Lock out over mid-foot."),
        Ex("Seated Dumbbell Press", MuscleGroup.SHOULDERS, Equipment.DUMBBELL, "Seated with back support, press the dumbbells from shoulder height to overhead."),
        Ex("Arnold Press", MuscleGroup.SHOULDERS, Equipment.DUMBBELL, "Start with palms facing you and rotate them outward as you press overhead."),
        Ex("Lateral Raise", MuscleGroup.SHOULDERS, Equipment.DUMBBELL, "With a slight bend in the elbows, raise the dumbbells out to the sides up to shoulder height."),
        Ex("Rear Delt Fly", MuscleGroup.SHOULDERS, Equipment.DUMBBELL, "Hinge forward and raise the dumbbells out to the sides, squeezing the back of your shoulders."),
        Ex("Cable Lateral Raise", MuscleGroup.SHOULDERS, Equipment.CABLE, "Using a low pulley, raise the handle across and out to the side to shoulder height."),
        Ex("Shrug", MuscleGroup.SHOULDERS, Equipment.DUMBBELL, "Hold heavy dumbbells and lift your shoulders straight up towards your ears, pause and lower."),

        // Arms
        Ex("Barbell Curl", MuscleGroup.ARMS, Equipment.BARBELL, "Elbows at your sides, curl the bar up to your shoulders without swinging, then lower slowly."),
        Ex("Dumbbell Curl", MuscleGroup.ARMS, Equipment.DUMBBELL, "Curl the dumbbells up, turning your palms up as you lift, then lower with control."),
        Ex("Hammer Curl", MuscleGroup.ARMS, Equipment.DUMBBELL, "Curl with palms facing each other (neutral grip)."),
        Ex("Preacher Curl", MuscleGroup.ARMS, Equipment.BARBELL, "Arms resting on the preacher pad, curl the bar up and lower until your arms are almost straight."),
        Ex("Cable Curl", MuscleGroup.ARMS, Equipment.CABLE, "Using a low pulley, curl the bar up keeping your elbows still."),
        Ex("Tricep Pushdown", MuscleGroup.ARMS, Equipment.CABLE, "Elbows at your sides, push the bar or rope down until your arms are straight."),
        Ex("Overhead Tricep Extension", MuscleGroup.ARMS, Equipment.DUMBBELL, "Hold one dumbbell overhead with both hands and lower it behind your head, then extend."),
        Ex("Skull Crusher", MuscleGroup.ARMS, Equipment.BARBELL, "Lying on a bench, lower the bar towards your forehead by bending the elbows, then extend."),
        Ex("Close-Grip Bench Press", MuscleGroup.ARMS, Equipment.BARBELL, "Bench press with hands about shoulder-width apart, keeping elbows close to your body."),

        // Core
        Ex("Plank", MuscleGroup.CORE, Equipment.BODYWEIGHT, "Hold a straight line from head to heels on your forearms. Log the seconds held as reps."),
        Ex("Hanging Leg Raise", MuscleGroup.CORE, Equipment.BODYWEIGHT, "Hang from a bar and raise your legs until they are parallel to the floor or higher, without swinging."),
        Ex("Cable Crunch", MuscleGroup.CORE, Equipment.CABLE, "Kneel facing a high pulley with a rope behind your head and crunch down, bringing elbows to knees."),
        Ex("Russian Twist", MuscleGroup.CORE, Equipment.BODYWEIGHT, "Sit with feet off the floor and rotate your torso side to side."),
        Ex("Ab Wheel Rollout", MuscleGroup.CORE, Equipment.OTHER, "Kneeling, roll the wheel forward as far as you can with a braced core, then pull back."),

        // Full body / cardio
        Ex("Kettlebell Swing", MuscleGroup.FULL_BODY, Equipment.KETTLEBELL, "Hinge at the hips and swing the kettlebell to chest height using a powerful hip drive."),
        Ex("Power Clean", MuscleGroup.FULL_BODY, Equipment.BARBELL, "Pull the bar explosively from the floor and catch it on the front of your shoulders in a quarter squat."),
        Ex("Farmer's Walk", MuscleGroup.FULL_BODY, Equipment.DUMBBELL, "Carry heavy dumbbells at your sides for distance or time while standing tall."),
        Ex("Burpee", MuscleGroup.CARDIO, Equipment.BODYWEIGHT, "Drop into a push-up, jump your feet back in and jump up with arms overhead."),
        Ex("Rowing Machine", MuscleGroup.CARDIO, Equipment.MACHINE, "Drive with the legs, then lean back and pull the handle to your ribs. Log minutes as reps."),
        Ex("Treadmill Run", MuscleGroup.CARDIO, Equipment.MACHINE, "Run at a steady pace. Log minutes as reps.")
    )

    private val programs = listOf(
        Program(
            "Full Body A", "Beginner full-body session — squat, push and pull.",
            listOf(
                Item("Back Squat", 3, "8-10", 120),
                Item("Bench Press", 3, "8-10", 120),
                Item("Barbell Row", 3, "8-10", 90),
                Item("Overhead Press", 2, "10", 90),
                Item("Plank", 3, "30-60", 60)
            )
        ),
        Program(
            "Full Body B", "Beginner full-body session — hinge, incline and vertical pull.",
            listOf(
                Item("Deadlift", 3, "5", 150),
                Item("Incline Dumbbell Press", 3, "10", 90),
                Item("Lat Pulldown", 3, "10", 90),
                Item("Walking Lunge", 2, "12", 90),
                Item("Hanging Leg Raise", 3, "10", 60)
            )
        ),
        Program(
            "Push Day", "Chest, shoulders and triceps.",
            listOf(
                Item("Bench Press", 4, "6-8", 150),
                Item("Overhead Press", 3, "8", 120),
                Item("Incline Dumbbell Press", 3, "10", 90),
                Item("Lateral Raise", 3, "12-15", 60),
                Item("Tricep Pushdown", 3, "12", 60),
                Item("Overhead Tricep Extension", 2, "12", 60)
            )
        ),
        Program(
            "Pull Day", "Back and biceps.",
            listOf(
                Item("Deadlift", 3, "5", 180),
                Item("Pull-Up", 3, "6-10", 120),
                Item("Barbell Row", 3, "8", 90),
                Item("Face Pull", 3, "15", 60),
                Item("Barbell Curl", 3, "10", 60),
                Item("Hammer Curl", 2, "12", 60)
            )
        ),
        Program(
            "Leg Day", "Quads, hamstrings, glutes and calves.",
            listOf(
                Item("Back Squat", 4, "6-8", 180),
                Item("Romanian Deadlift", 3, "8-10", 120),
                Item("Leg Press", 3, "10-12", 90),
                Item("Lying Leg Curl", 3, "12", 60),
                Item("Standing Calf Raise", 4, "12-15", 60)
            )
        ),
        Program(
            "Upper Body Strength", "Heavy upper-body work with lower reps.",
            listOf(
                Item("Bench Press", 4, "5", 180),
                Item("Barbell Row", 4, "6", 150),
                Item("Overhead Press", 3, "6", 150),
                Item("Pull-Up", 3, "8", 120),
                Item("Dumbbell Curl", 2, "10", 60),
                Item("Dips", 2, "10", 90)
            )
        )
    )
}
