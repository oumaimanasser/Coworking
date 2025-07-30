namespace CoworkingApp.Models
{
    public class Creneau
    {
        public int Id { get; set; }
        public DateTime Date { get; set; }
        public TimeSpan HeureDebut { get; set; }
        public TimeSpan HeureFin { get; set; }
        public bool Disponible { get; set; } = true;
    }
}
