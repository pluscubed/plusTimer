package com.pluscubed.plustimer.model;

import com.firebase.client.Firebase;
import com.pluscubed.plustimer.App;

import rx.Single;

public class FirebaseDbUtil {

    protected static Single<Firebase> getSolveRef(String sessionId, String solveId) {
        return getSolvesRef(sessionId).map(firebase -> firebase.child(solveId));
    }

    protected static Single<Firebase> getSolvesRef(String sessionId) {
        return App.getFirebaseUserRef().map(firebase -> firebase.child("solves").child(sessionId));
    }

    protected static Single<Firebase> getSessionRef(String puzzleTypeId, String sessionId) {
        return getSessionsRef(puzzleTypeId).map(firebase -> firebase.child(sessionId));
    }

    protected static Single<Firebase> getSessionsRef(String puzzleTypeId) {
        return App.getFirebaseUserRef().map(firebase -> firebase.child("sessions").child(puzzleTypeId));
    }

    protected static Single<Firebase> getPuzzleTypeRef(String puzzleTypeId) {
        return getPuzzleTypesRef().map(firebase -> firebase.child(puzzleTypeId));
    }

    protected static Single<Firebase> getPuzzleTypesRef() {
        return App.getFirebaseUserRef().map(firebase -> firebase.child("puzzletypes"));
    }
}
