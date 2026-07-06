import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.4/firebase-app.js";
import {
  getFirestore,
  doc,
  onSnapshot
} from "https://www.gstatic.com/firebasejs/10.12.4/firebase-firestore.js";

const firebaseConfig = {
  apiKey: "AIzaSyCq22ptORXfwvdXWuBoU4ayZaKjzHY_4Eg",
  authDomain: "shieldup-925d1.firebaseapp.com",
  projectId: "shieldup-925d1",
  storageBucket: "shieldup-925d1.firebasestorage.app",
  messagingSenderId: "1095355300936",
  appId: "1:1095355300936:web:5fe45369cc6c33f9b46c7d"
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

const info = document.getElementById("info");

const params = new URLSearchParams(window.location.search);
const sessionId = params.get("id");

let map = null;
let marker = null;

function setInfo(text, isError = false) {
  info.textContent = text;
  info.className = isError ? "error" : "";
}

function waitForGoogleMaps(callback) {
  if (window.google && window.google.maps) {
    callback();
    return;
  }

  setTimeout(() => waitForGoogleMaps(callback), 200);
}

function initMap(latitude, longitude, userName) {
  const position = { lat: latitude, lng: longitude };

  map = new google.maps.Map(document.getElementById("map"), {
    center: position,
    zoom: 17,
    mapTypeControl: false,
    streetViewControl: false,
    fullscreenControl: false
  });

  marker = new google.maps.Marker({
    position,
    map,
    title: userName || "Utente ShieldUp"
  });
}

function updateMap(latitude, longitude, userName) {
  const position = { lat: latitude, lng: longitude };

  if (!map || !marker) {
    waitForGoogleMaps(() => initMap(latitude, longitude, userName));
    return;
  }

  marker.setPosition(position);
  map.panTo(position);
}

if (!sessionId) {
  setInfo("Link non valido: sessione mancante.", true);
} else {
  const sessionRef = doc(db, "liveLocations", sessionId);

  onSnapshot(
    sessionRef,
    (snapshot) => {
      if (!snapshot.exists()) {
        setInfo("Sessione non trovata.", true);
        return;
      }

      const data = snapshot.data();

      if (!data.active) {
        setInfo("La condivisione della posizione è terminata.");
        return;
      }

      const latitude = data.latitude;
      const longitude = data.longitude;
      const userName = data.userName || "Utente ShieldUp";
      const destination = data.destination || "destinazione non specificata";

      if (typeof latitude !== "number" || typeof longitude !== "number") {
        setInfo("Posizione non ancora disponibile.");
        return;
      }

      const updatedDate = data.updatedAt
        ? new Date(data.updatedAt).toLocaleTimeString("it-IT")
        : "";

      setInfo(`${userName} sta andando verso: ${destination}. Ultimo aggiornamento: ${updatedDate}`);

      updateMap(latitude, longitude, userName);
    },
    (error) => {
      console.error(error);
      setInfo("Errore nel caricamento della posizione.", true);
    }
  );
}